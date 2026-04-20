# Expense Tracker (Phase 2b) — Design Spec

**Date:** 2026-04-21
**Project:** `/Users/macbookpro/Documents/digikhata-clone`
**Depends on:** Phase 2a Cash Register (Room v2)
**Goal:** Add a per-book Expense Tracker — an independent log of money spent on business costs — wired to the existing "Expense" bottom-nav tab (currently a Coming Soon placeholder).

---

## Overview

Expense Tracker is a standalone stream of expense entries tied to a Business. It is **not** linked to the Cash Register or the customer ledger: logging "Rent Rs 20,000" as an Expense does NOT create a matching Cash Out entry. Users pick the tool that matches how they think about a given cost.

Each entry records amount, category (from a fixed preset list), payment method (Cash/Bank/Card/Digital), date, an optional note, and an optional receipt photo. The main screen shows a single "Total Spent" number for the selected period, a filter chip row, the entry list, and a FAB to add a new expense.

---

## Decisions (from brainstorming)

| # | Decision |
|---|---|
| Q1 | Independent stream — separate entity, no auto-sync with Cash Register |
| Q2 | Entry fields: amount, category, date, note, photo, **+ payment method** |
| Q3 | Fixed preset list of 10 categories (expense-specific, no income categories) |
| Q4 | Summary card = single "Total Spent" number (no category breakdown, no chart) |
| Q5 | Tap entry → detail screen → Edit + Delete (mirrors Cash Register) |

---

## Data Model

### New entity — `ExpenseEntry`

File: `app/src/main/java/com/digikhata/data/entity/ExpenseEntry.kt`

```
id: Long             PK, autoGenerate, default 0
businessId: Long     FK → businesses.id, indexed, onDelete=CASCADE
amount: Double       always positive
category: String     one of ExpenseCategories.keys
paymentMethod: String one of "cash","bank","card","digital"
note: String?
entryDate: Long      millis since epoch
imageLocalPath: String?
createdAt: Long
updatedAt: Long
```

No `type` column — expenses are always outflow.

### Category presets (10)

File: `app/src/main/java/com/digikhata/ui/expense/ExpenseCategories.kt`

| key | label | Material icon |
|---|---|---|
| rent | Rent | Home |
| utilities | Utilities | Bolt |
| salaries | Salaries | Badge |
| supplies | Office Supplies | Inventory2 |
| travel | Travel | LocalShipping |
| food | Food & Meals | Restaurant |
| marketing | Marketing | Campaign |
| repairs | Repairs | Build |
| tax | Tax & Fees | AccountBalance |
| other | Other | MoreHoriz |

Helpers: `fun labelOf(key: String): String`, `fun iconOf(key: String): ImageVector`.

### Payment methods

File: `app/src/main/java/com/digikhata/ui/expense/PaymentMethod.kt`

```kotlin
enum class PaymentMethod(val key: String, val label: String, val icon: ImageVector) {
    CASH("cash", "Cash", Icons.Default.Payments),
    BANK("bank", "Bank", Icons.Default.AccountBalance),
    CARD("card", "Card", Icons.Default.CreditCard),
    DIGITAL("digital", "Digital", Icons.Default.Smartphone);
    companion object {
        fun fromKey(key: String): PaymentMethod = values().firstOrNull { it.key == key } ?: CASH
    }
}
```

### DAO — `ExpenseEntryDao`

File: `app/src/main/java/com/digikhata/data/dao/ExpenseEntryDao.kt`

- `@Insert suspend fun insert(entry: ExpenseEntry): Long`
- `@Update suspend fun update(entry: ExpenseEntry)`
- `@Delete suspend fun delete(entry: ExpenseEntry)`
- `@Query("SELECT * FROM expense_entries WHERE id = :id") fun getById(id: Long): Flow<ExpenseEntry?>`
- `@Query("SELECT * FROM expense_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to ORDER BY entryDate DESC, id DESC") fun getInRange(bid: Long, from: Long, to: Long): Flow<List<ExpenseEntry>>`
- `@Query("SELECT COALESCE(SUM(amount),0) FROM expense_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to") fun totalForPeriod(bid: Long, from: Long, to: Long): Flow<Double>`

### Repository additions — `DigiRepository`

```
fun expenses(businessId: Long, from: Long, to: Long): Flow<List<ExpenseEntry>>
fun expenseTotal(businessId: Long, from: Long, to: Long): Flow<Double>
fun getExpense(id: Long): Flow<ExpenseEntry?>
suspend fun addExpense(entry: ExpenseEntry, imagePath: String?): Long
suspend fun updateExpense(entry: ExpenseEntry)
suspend fun deleteExpense(entry: ExpenseEntry)   // also deletes image file if present
```

### Migration

`DigiDatabase` — `version = 3`. Entity list grows by one (`ExpenseEntry`). Add `MIGRATION_2_3` to `data/Migrations.kt`:

```sql
CREATE TABLE IF NOT EXISTS `expense_entries` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `businessId` INTEGER NOT NULL,
  `amount` REAL NOT NULL,
  `category` TEXT NOT NULL,
  `paymentMethod` TEXT NOT NULL,
  `note` TEXT,
  `entryDate` INTEGER NOT NULL,
  `imageLocalPath` TEXT,
  `createdAt` INTEGER NOT NULL,
  `updatedAt` INTEGER NOT NULL,
  FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_expense_entries_businessId` ON `expense_entries` (`businessId`);
```

Register via `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)` in `DatabaseModule.provideDatabase`. Keep the existing `.fallbackToDestructiveMigration()` as dev-only safety.

---

## Screens & Navigation

### Routing changes

`ui/navigation/Routes.kt`:
- `const val EXPENSE = "expense"`
- `const val EXPENSE_DETAIL_PATTERN = "expenseDetail/{entryId}"`
- `fun expenseDetail(id: Long) = "expenseDetail/$id"`

`ui/navigation/DigiNavGraph.kt`:
- Find the bottom-nav tab currently routing "Expense" to `comingSoon/Expense` — change destination to `Routes.EXPENSE`.
- Add `composable(Routes.EXPENSE) { ExpenseScreen(navController) }`.
- Add `composable(Routes.EXPENSE_DETAIL_PATTERN, arguments = listOf(navArgument("entryId") { type = NavType.LongType })) { ExpenseDetailScreen(navController) }`.
- Extend the bottom-nav chrome visibility rule to include `expense` (same as `cash`, `home`).

### Files under `com.digikhata.ui.expense/`

| File | Responsibility | ~lines |
|---|---|---|
| `ExpenseCategories.kt` | 10 presets + labelOf/iconOf | 40 |
| `PaymentMethod.kt` | Enum + fromKey | 30 |
| `ExpenseFilter.kt` | `enum class ExpenseFilter { TODAY, WEEK, MONTH, ALL }` + `range(now)` helper | 40 |
| `ExpenseViewModel.kt` | @HiltViewModel; `entries`, `total`, `filter`, `currency` StateFlows | 80 |
| `ExpenseScreen.kt` | Scaffold + "Total Spent" card + filter chips + LazyColumn + FAB + sheet state | 150 |
| `ExpenseRow.kt` | Row: icon, label+date, payment chip, amount right-aligned red; optional 40dp thumbnail | 70 |
| `AddExpenseSheet.kt` | ModalBottomSheet: amount / category / payment / date / note / photo / Save | 170 |
| `ExpenseCategoryPicker.kt` | Bottom-sheet list of 10 categories | 50 |
| `PaymentMethodPicker.kt` | Bottom-sheet list of 4 methods | 50 |
| `ExpenseSheetViewModel.kt` | Thin VM: `suspend fun save(entry, imagePath)` / `suspend fun update(entry)` | 40 |
| `ExpenseDetailViewModel.kt` | Reads `entryId` from SavedStateHandle; `entry`, `currency`; `suspend fun delete(entry)` | 40 |
| `ExpenseDetailScreen.kt` | Top bar (back + delete), big red amount, category chip, payment chip, date, note, full-width photo (tap → ZoomableImageDialog), Edit button opens AddExpenseSheet pre-filled, Delete confirmation dialog | 140 |

### Summary card layout

A single `Card(colorScheme.surface)`, 12dp padding, centered content:

```
┌──────────────────────────────────┐
│           TOTAL SPENT            │
│          Rs 50,000.00            │  (big, red, titleLarge+)
│            April 2026            │  (filter label, labelSmall, onSurfaceVariant)
└──────────────────────────────────┘
```

Filter chip row (`FilterChip`s: Today / Week / Month / All) sits directly above. Default: **This Month**.

### Add expense flow

1. FAB tap → `AddExpenseSheet` opens (sheet header: "Add Expense", red dot).
2. Fields (top to bottom):
   - Amount — numeric kb, autofocus, validate > 0
   - Category — row opens `ExpenseCategoryPicker` (default "Other")
   - Payment method — row opens `PaymentMethodPicker` (default CASH)
   - Date — tap opens `DatePickerDialog`, default today
   - Note — optional
   - Photo row — Camera / Gallery via existing `PhotoPicker`
3. Save disabled until amount > 0. On save, image copied via `ImageUtils.saveImageToAppDir`, `repo.addExpense` called, sheet closes.

### Edit / Delete

- Edit button on detail screen opens `AddExpenseSheet` in edit mode (pre-filled, calls `repo.updateExpense` on save).
- Delete button → `AlertDialog("Delete this expense?")` → on confirm, `repo.deleteExpense` → `navController.navigateUp()`. Repo deletes the row and, if `imageLocalPath != null`, `File(path).delete()`.

---

## Dependency Injection

`di/DatabaseModule.kt` additions:
- `@Provides fun provideExpenseEntryDao(db: DigiDatabase) = db.expenseEntryDao()`
- Add `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)` to the Room builder

`DigiDatabase.kt`:
- Add `ExpenseEntry::class` to entities
- Bump `version = 3`
- `abstract fun expenseEntryDao(): ExpenseEntryDao`

`DigiRepositoryImpl` — inject `expenseEntryDao: ExpenseEntryDao` as an added constructor parameter; implement the 6 new repository methods.

---

## Permissions

No new permissions. CAMERA and READ_MEDIA_IMAGES already declared for customer-ledger photo receipts are reused here.

---

## What Phase 2b Does NOT Include

- PDF export of expenses
- Category breakdown chart / reports screen
- Recurring expenses
- Vendor/payee field (free text) — can be added in a follow-up
- Auto-sync with Cash Register
- Multi-currency per entry (uses the book's currency)

These are candidates for a future 2b.1 once the core ships.

---

## Success Criteria

- Bottom-nav "Expense" tab opens `ExpenseScreen` (no longer shows ComingSoon).
- User can add an expense with a payment method; it appears immediately in the list and the "Total Spent" summary updates.
- Adding an expense with a photo shows a thumbnail on the row and full-screen on detail (tap to zoom).
- Filter chips change the date window; total and list update accordingly.
- Editing an expense updates both list and total.
- Deleting an expense removes the row, updates total, and removes the image file from disk.
- Switching books switches the expense list (entries scoped to `businessId`).
- App still builds: `./gradlew assembleDebug` → SUCCESS.
- Room migration 2→3 runs cleanly on upgrades (no data loss on the 6 prior tables).
