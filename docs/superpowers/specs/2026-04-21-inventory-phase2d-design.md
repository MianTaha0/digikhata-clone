# Inventory / Stock (Phase 2d) — Design Spec

**Date:** 2026-04-21
**Project:** `/Users/macbookpro/Documents/digikhata-clone`
**Depends on:** Phase 2a–2c (Room v4)
**Goal:** Add a per-book Inventory/Stock module — a product catalog with cost/sell prices, current stock, manual adjustments with an audit log, and low-stock flagging.

---

## Overview

Inventory is an independent per-business catalog of products. Each product has a cost price, a sell price, a current stock quantity, an optional low-stock threshold, an optional SKU, an optional photo, and a free-text unit of measure (default "pcs"). Stock changes happen only through explicit manual adjustments, each of which writes a `StockMovement` row so users can audit the history.

Inventory is **not** coupled to Invoicing — logging an invoice does NOT decrement stock. This matches the "independent streams" pattern used by Cash Register and Expense Tracker.

---

## Decisions (from brainstorming)

| # | Decision |
|---|---|
| Q1 | Product carries BOTH costPrice and sellPrice (margin-aware) |
| Q2 | Stock changes via manual adjust only; every change logs a StockMovement |
| Q3 | Low-stock badge + filter chips (All / Low / Out) |
| Q4 | Tap item → detail screen with movements history + Edit/Delete |
| Q5 | Optional product photo |
| Q6 | Dashboard summary: Items count / Stock Value (Σ qty×cost) / Low-stock count |

---

## Data Model

### New entity — `Product`

File: `app/src/main/java/com/digikhata/data/entity/Product.kt`

```
id: Long              PK, autoGenerate, default 0
businessId: Long      FK → businesses.id, indexed, onDelete=CASCADE
name: String
sku: String?          nullable
costPrice: Double     what was paid (for value + margin)
sellPrice: Double     what is charged (display)
quantity: Double      current stock; Double so kg/ltr work
lowStockThreshold: Double   0 disables the badge
unit: String          free text, default "pcs"
imageLocalPath: String?
createdAt: Long
updatedAt: Long
```

Table name: `products`.

### New entity — `StockMovement`

File: `app/src/main/java/com/digikhata/data/entity/StockMovement.kt`

```
id: Long              PK, autoGenerate, default 0
productId: Long       FK → products.id, indexed, onDelete=CASCADE
delta: Double         positive = stock in, negative = stock out
reason: String?       e.g. "Purchase", "Sale", "Waste", "Correction", "Other", or free text
createdAt: Long
```

Table name: `stock_movements`.

### Stock reason presets

File: `app/src/main/java/com/digikhata/ui/inventory/StockReasons.kt`

```kotlin
val STOCK_REASONS = listOf("Purchase", "Sale", "Waste", "Correction", "Other")
```

Used by the adjust-stock sheet dropdown. Stored as plain strings on the movement row (free-text allowed).

### DAO — `ProductDao`

File: `app/src/main/java/com/digikhata/data/dao/ProductDao.kt`

- `@Insert suspend fun insert(p: Product): Long`
- `@Update suspend fun update(p: Product)`
- `@Delete suspend fun delete(p: Product)`
- `@Query("SELECT * FROM products WHERE id = :id") fun getById(id: Long): Flow<Product?>`
- `@Query("SELECT * FROM products WHERE businessId = :bid ORDER BY name COLLATE NOCASE ASC") fun getByBusiness(bid: Long): Flow<List<Product>>`
- `@Query("SELECT COALESCE(SUM(quantity * costPrice), 0) FROM products WHERE businessId = :bid") fun totalValue(bid: Long): Flow<Double>`
- `@Query("SELECT COUNT(*) FROM products WHERE businessId = :bid AND lowStockThreshold > 0 AND quantity <= lowStockThreshold") fun lowStockCount(bid: Long): Flow<Int>`
- `@Query("SELECT COUNT(*) FROM products WHERE businessId = :bid") fun itemCount(bid: Long): Flow<Int>`

### DAO — `StockMovementDao`

File: `app/src/main/java/com/digikhata/data/dao/StockMovementDao.kt`

- `@Insert suspend fun insert(m: StockMovement): Long`
- `@Query("SELECT * FROM stock_movements WHERE productId = :pid ORDER BY createdAt DESC, id DESC") fun getByProduct(pid: Long): Flow<List<StockMovement>>`

### Repository additions — `DigiRepository`

```
fun products(businessId: Long): Flow<List<Product>>
fun getProduct(id: Long): Flow<Product?>
fun productMovements(productId: Long): Flow<List<StockMovement>>
fun inventoryItemCount(businessId: Long): Flow<Int>
fun inventoryTotalValue(businessId: Long): Flow<Double>
fun lowStockCount(businessId: Long): Flow<Int>
suspend fun addProduct(product: Product, imagePath: String?): Long
suspend fun updateProduct(product: Product)
suspend fun deleteProduct(product: Product)          // also deletes imageLocalPath file; movements cascade
suspend fun adjustStock(productId: Long, delta: Double, reason: String?) // transactional: insert movement + bump product.quantity + updatedAt
```

`adjustStock` must use `db.withTransaction { ... }` — mirrors `saveInvoice` in Phase 2c.

### Migration

`DigiDatabase` — `version = 5`. Entity list grows by two. Add `MIGRATION_4_5` to `data/Migrations.kt`:

```sql
CREATE TABLE IF NOT EXISTS `products` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `businessId` INTEGER NOT NULL,
  `name` TEXT NOT NULL,
  `sku` TEXT,
  `costPrice` REAL NOT NULL,
  `sellPrice` REAL NOT NULL,
  `quantity` REAL NOT NULL,
  `lowStockThreshold` REAL NOT NULL,
  `unit` TEXT NOT NULL,
  `imageLocalPath` TEXT,
  `createdAt` INTEGER NOT NULL,
  `updatedAt` INTEGER NOT NULL,
  FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_products_businessId` ON `products` (`businessId`);

CREATE TABLE IF NOT EXISTS `stock_movements` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `productId` INTEGER NOT NULL,
  `delta` REAL NOT NULL,
  `reason` TEXT,
  `createdAt` INTEGER NOT NULL,
  FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`);
```

Register via `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)` in `DatabaseModule.provideDatabase`.

---

## Screens & Navigation

### Routing changes

`ui/navigation/Routes.kt`:
- `const val INVENTORY = "inventory"`
- `const val PRODUCT_DETAIL_PATTERN = "productDetail/{productId}"`
- `fun productDetail(id: Long) = "productDetail/$id"`

`ui/navigation/DigiNavGraph.kt`:
- Find the bottom-nav tab currently routing "Stock" (or "Inventory") to `comingSoon/...` — change destination to `Routes.INVENTORY`.
- Add `composable(Routes.INVENTORY) { InventoryScreen(navController) }`.
- Add `composable(Routes.PRODUCT_DETAIL_PATTERN, arguments = listOf(navArgument("productId") { type = NavType.LongType })) { ProductDetailScreen(navController) }`.
- Extend bottom-nav chrome visibility rule to include `inventory` (same treatment as cash/expense/home/invoices).

### Files under `com.digikhata.ui.inventory/`

| File | Responsibility | ~lines |
|---|---|---|
| `InventoryFilter.kt` | `enum class InventoryFilter { ALL, LOW, OUT }` + label helper | 30 |
| `StockReasons.kt` | preset list | 15 |
| `InventoryViewModel.kt` | @HiltViewModel; `products` (filtered), `itemCount`, `totalValue`, `lowCount`, `filter`, `currency` StateFlows | 90 |
| `InventoryScreen.kt` | Scaffold + `InventorySummaryCard` + filter chips + LazyColumn of `ProductRow` + FAB + AddEditProductSheet state | 160 |
| `InventorySummaryCard.kt` | 3-column card: Items / Stock Value / Low Stock | 60 |
| `ProductRow.kt` | 40dp thumbnail, name + optional sku (labelSmall), right column qty + unit (titleMedium) + sellPrice (labelSmall); red dot badge if low | 80 |
| `AddEditProductSheet.kt` | ModalBottomSheet: name, sku, costPrice, sellPrice, quantity (initial, edit-mode shows current), lowStockThreshold, unit, photo row, Save | 200 |
| `ProductSheetViewModel.kt` | `suspend fun save(product, imagePath)` / `suspend fun update(product)` | 40 |
| `ProductDetailViewModel.kt` | SavedStateHandle → productId; `product`, `movements`, `currency`; `suspend fun delete(product)`, `suspend fun adjust(delta, reason)` | 60 |
| `ProductDetailScreen.kt` | Top bar (back + delete); photo (tap → ZoomableImageDialog if exists, else full-width); name + sku; stock block (big qty × unit, red low badge); cost / sell / margin row; "Adjust Stock" button opens sheet; Movements list; Edit FAB opens AddEditProductSheet pre-filled; Delete confirmation dialog | 190 |
| `AdjustStockSheet.kt` | ModalBottomSheet: +/- segmented toggle, amount numeric field, reason dropdown (ExposedDropdownMenuBox, STOCK_REASONS + free text), optional note appended to reason, Save | 140 |
| `MovementRow.kt` | Date + reason on left; delta right-aligned (green prefix "+", red prefix "−") | 50 |

### Inventory summary card layout

`Card(colorScheme.surface)`, 12dp padding:

```
┌──────────────────────────────────────────┐
│   12         Rs 34,500          3        │
│   Items      Stock Value        Low      │
└──────────────────────────────────────────┘
```

Filter chip row (`FilterChip`: All / Low / Out) directly below the card. Default: **All**.

Filter semantics:
- **ALL** — every product
- **LOW** — `lowStockThreshold > 0 AND quantity <= lowStockThreshold AND quantity > 0`
- **OUT** — `quantity <= 0`

Filtering is done client-side in the ViewModel over the full `products` list (avoids DAO combinatorics).

### Add / edit product flow

1. FAB tap → `AddEditProductSheet` opens.
2. Fields (top to bottom):
   - Name — text, autofocus, required
   - SKU — text, optional
   - Cost Price — numeric, required, ≥ 0
   - Sell Price — numeric, required, ≥ 0
   - Initial Quantity — numeric, required (edit mode: shows current, disabled — use Adjust Stock to change)
   - Low-stock Threshold — numeric, default 0
   - Unit — text, default "pcs"
   - Photo row — Camera / Gallery via existing `PhotoPicker`
3. Save disabled until name non-blank and cost/sell/quantity are valid numbers. On save (new), `repo.addProduct` is called. On save (edit), `repo.updateProduct` is called (quantity unchanged).

### Adjust stock flow

1. Detail screen "Adjust Stock" button → `AdjustStockSheet`.
2. Segmented toggle: **Stock In (+)** / **Stock Out (−)**, default In.
3. Amount numeric field (> 0).
4. Reason dropdown (STOCK_REASONS) — default "Purchase" if In, "Sale" if Out.
5. Optional note appended as `"$reason — $note"`.
6. On save: `repo.adjustStock(productId, signedDelta, reason)` — transactional write of movement + product update.

### Delete

Detail screen top-bar delete → `AlertDialog("Delete this product?")` → on confirm, `repo.deleteProduct` → `navController.navigateUp()`. Repo deletes the row (movements cascade) and, if `imageLocalPath != null`, `File(path).delete()`.

---

## Dependency Injection

`di/DatabaseModule.kt` additions:
- `@Provides fun provideProductDao(db: DigiDatabase) = db.productDao()`
- `@Provides fun provideStockMovementDao(db: DigiDatabase) = db.stockMovementDao()`
- `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)`

`DigiDatabase.kt`:
- Add `Product::class`, `StockMovement::class` to entities
- Bump `version = 5`
- `abstract fun productDao(): ProductDao`
- `abstract fun stockMovementDao(): StockMovementDao`

`DigiRepositoryImpl` — inject `productDao`, `stockMovementDao`, and (already present) `DigiDatabase db` for `withTransaction`. Implement the 10 new methods.

---

## Permissions

No new permissions. CAMERA and READ_MEDIA_IMAGES are reused for product photos.

---

## What Phase 2d Does NOT Include

- Variants (size/color)
- Categories / tagging
- Supplier linking (use customer/supplier ledger separately)
- Auto-decrement from invoices
- Barcode scanning
- CSV import/export
- Price history / cost history
- Low-stock push notifications
- Stock valuation methods (FIFO/LIFO) — always cost × qty

These are candidates for 2d.1.

---

## Success Criteria

- Bottom-nav "Stock" tab opens `InventoryScreen` (no longer ComingSoon).
- User can add a product with cost + sell + initial quantity + optional photo; it appears in the list immediately and the summary card updates.
- Filter chips (All / Low / Out) change which rows are visible and the empty-state copy reflects the filter.
- Tapping a product opens the detail screen; tapping the photo opens zoom (if component exists).
- Adjust Stock with "+5 Purchase" increases quantity by 5 and logs a movement. "−2 Sale" decreases by 2. Both appear in the Movements list newest-first.
- Editing a product updates fields but NOT quantity (must use Adjust Stock).
- Deleting a product removes the row from list, deletes the image file, and cascades its movements.
- Switching books switches the product list.
- Summary card numbers are live (react to adjustments and new products).
- App still builds: `./gradlew assembleDebug` → SUCCESS.
- Room migration 4→5 runs cleanly on upgrades (no data loss on the 8 prior tables).
