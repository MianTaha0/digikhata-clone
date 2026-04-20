# Cash Register (Phase 2a) — Design Spec

**Date:** 2026-04-18
**Project:** `/Users/macbookpro/Documents/digikhata-clone`
**Depends on:** Phase 1 foundation (commit `965bf5a`)
**Goal:** Add a per-book Cash Register — an independent log of cash flowing in and out of the business — wired to the existing "Cash" bottom-nav tab (currently a Coming Soon placeholder).

---

## Overview

Cash Register is a standalone stream of cash-in / cash-out entries tied to a Business. It is **not** linked to the customer ledger: a "You Got Rs 500" customer transaction does NOT auto-create a cash-in entry. Users keep two mental books and we respect that.

Each entry records amount, type (in or out), a category from a fixed preset list, a date, an optional note, and an optional receipt photo.

The main screen shows a summary card (Cash In / Cash Out / Net) for a selected period, filter chips to scope the period, the entry list, and two bottom buttons to add a new entry.

---

## Decisions (from brainstorming)

| # | Decision |
|---|---|
| Q2 | Independent streams — no auto-sync with customer ledger |
| Q3 | Per-book (each Business has its own register) |
| Q4 | Fixed preset category list (no user-defined categories) |
| Q5 | Entry fields: amount, type, category, date, note, **receipt photo** |
| Q6 | Summary card (Cash In / Cash Out / Net) + filter chips + list + two bottom buttons |
| Q7 | Tap entry → full detail screen with Edit + Delete |
| Q8 | No PDF export, no reports screen in 2a |

---

## Data Model

### New entity — `CashEntry`

File: `app/src/main/java/com/digikhata/data/entity/CashEntry.kt`

```
id: Long             PK, autoGenerate, default 0
businessId: Long     FK → businesses.id, indexed, onDelete=CASCADE
amount: Double       always positive
type: Int            0 = Cash Out, 1 = Cash In   (same convention as TxEntity)
category: String     one of CashCategories.keys
note: String?
entryDate: Long      millis since epoch
imageLocalPath: String?
createdAt: Long
updatedAt: Long
```

### Category presets

File: `app/src/main/java/com/digikhata/ui/cash/CashCategories.kt`

Fixed list of 10:

| key | label | Material icon |
|---|---|---|
| sales | Sales | ShoppingCart |
| purchase | Purchase | Inventory2 |
| salary | Salary | Badge |
| rent | Rent | Home |
| utilities | Utilities | Bolt |
| transport | Transport | LocalShipping |
| food | Food | Restaurant |
| maintenance | Maintenance | Build |
| loan | Loan | AccountBalance |
| other | Other | MoreHoriz |

Data class: `data class CashCategory(val key: String, val label: String, val icon: ImageVector)`. Helper: `fun labelOf(key: String): String`.

### DAO — `CashEntryDao`

File: `app/src/main/java/com/digikhata/data/dao/CashEntryDao.kt`

- `@Insert suspend fun insert(entry: CashEntry): Long`
- `@Update suspend fun update(entry: CashEntry)`
- `@Delete suspend fun delete(entry: CashEntry)`
- `@Query("SELECT * FROM cash_entries WHERE id = :id") fun getById(id: Long): Flow<CashEntry?>`
- `@Query("SELECT * FROM cash_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to ORDER BY entryDate DESC, id DESC") fun getInRange(bid: Long, from: Long, to: Long): Flow<List<CashEntry>>`
- `@Query("SELECT COALESCE(SUM(CASE WHEN type=1 THEN amount ELSE 0 END),0) as totalIn, COALESCE(SUM(CASE WHEN type=0 THEN amount ELSE 0 END),0) as totalOut FROM cash_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to") fun totalsForPeriod(bid: Long, from: Long, to: Long): Flow<CashTotals>`

### Domain model — `CashTotals`

File: `app/src/main/java/com/digikhata/domain/model/CashTotals.kt`

```kotlin
data class CashTotals(val totalIn: Double = 0.0, val totalOut: Double = 0.0) {
    val net: Double get() = totalIn - totalOut
}
```

### Repository additions — `DigiRepository`

Add to interface + impl:

```
fun cashEntries(businessId: Long, from: Long, to: Long): Flow<List<CashEntry>>
fun cashTotals(businessId: Long, from: Long, to: Long): Flow<CashTotals>
fun getCashEntry(id: Long): Flow<CashEntry?>
suspend fun addCashEntry(entry: CashEntry, imagePath: String?): Long
suspend fun updateCashEntry(entry: CashEntry)
suspend fun deleteCashEntry(entry: CashEntry)  // also deletes image file off disk if present
```

### Database migration

`DigiDatabase` — `version = 2`. Entity list grows by one (`CashEntry`). Provide a `MIGRATION_1_2` that executes:

```sql
CREATE TABLE IF NOT EXISTS `cash_entries` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `businessId` INTEGER NOT NULL,
  `amount` REAL NOT NULL,
  `type` INTEGER NOT NULL,
  `category` TEXT NOT NULL,
  `note` TEXT,
  `entryDate` INTEGER NOT NULL,
  `imageLocalPath` TEXT,
  `createdAt` INTEGER NOT NULL,
  `updatedAt` INTEGER NOT NULL,
  FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_cash_entries_businessId` ON `cash_entries` (`businessId`);
```

Register the migration in `DatabaseModule.provideDatabase`. Keep `.fallbackToDestructiveMigration()` as a safety net for development only (Phase 1 already has it).

---

## Screens & Navigation

### Routing changes

`ui/navigation/Routes.kt`:
- `const val CASH = "cash"` (already exists as the tab label; wire the route)
- `const val CASH_ENTRY_DETAIL = "cashEntryDetail/{entryId}"`
- Helper `fun cashEntryDetail(id: Long) = "cashEntryDetail/$id"`

`ui/navigation/DigiNavGraph.kt`:
- The bottom-nav "Cash" item was routing to `comingSoon/Cash` — change it to navigate to `Routes.CASH`.
- Add `composable("cash") { CashRegisterScreen(navController) }`.
- Add `composable("cashEntryDetail/{entryId}", arguments=[navArgument("entryId"){type=NavType.LongType}]) { CashEntryDetailScreen(navController) }`.

### Files under `com.digikhata.ui.cash/`

| File | Responsibility | Approx. size |
|---|---|---|
| `CashCategories.kt` | Preset list + `labelOf(key)` + `iconOf(key)` | ~40 lines |
| `CashFilter.kt` | `enum class CashFilter { TODAY, WEEK, MONTH, ALL }` + helper `CashFilter.range(now: Long): Pair<Long, Long>` | ~40 lines |
| `CashRegisterViewModel.kt` | `@HiltViewModel` — exposes `entries: StateFlow<List<CashEntry>>`, `totals: StateFlow<CashTotals>`, `filter: StateFlow<CashFilter>`; `setFilter(f)` | ~80 lines |
| `CashRegisterScreen.kt` | Scaffold + top summary card + filter chip row + LazyColumn + two-button bottom bar + AddCashEntrySheet state | ~150 lines |
| `CashEntryRow.kt` | Single row: category icon + label + date (left) and amount (right, green if in, red if out); small thumbnail if imageLocalPath != null; taps to detail | ~60 lines |
| `AddCashEntrySheet.kt` | ModalBottomSheet: type (locked by caller), amount (numeric, autofocus), category picker, date picker, note, photo row; Save button | ~150 lines |
| `CashCategoryPicker.kt` | DropdownMenu or bottom-sheet list of CashCategories; shared by Add and Edit | ~50 lines |
| `CashEntryDetailViewModel.kt` | Loads one entry via `SavedStateHandle["entryId"]`; delete suspending fn | ~40 lines |
| `CashEntryDetailScreen.kt` | Full view (big amount, category, date, note, zoomable photo via existing `ZoomableImageDialog`), Edit button (opens AddCashEntrySheet pre-filled), Delete button (AlertDialog) | ~130 lines |

### Summary card layout

Matches the visual language of the existing customer Home totals strip. Three equally-spaced columns inside a single `Card(colorScheme.surface)` with 12dp padding:

```
┌──────────────────────────────────────────┐
│ CASH IN       CASH OUT       NET         │
│ Rs 45,000     Rs 32,500      Rs 12,500   │
│ (green)       (red)          (green/red) │
└──────────────────────────────────────────┘
```

Rendered with `Row` + three `Column(weight=1f)`. Label in `labelSmall onSurfaceVariant`, value in `titleMedium` bold with color.

### Filter chips

`FilterChip` row directly above the summary card: **Today**, **This Week**, **This Month** (default), **All**. Selected chip uses `colorScheme.primary` container.

### Bottom buttons

Two equally-weighted `Button`s at the bottom:
- **+ Cash In** — green (`DigiGreen`), leading icon `Icons.Default.ArrowDownward`
- **+ Cash Out** — red (`DigiRed`), leading icon `Icons.Default.ArrowUpward`

Tapping either opens `AddCashEntrySheet` with the type pre-selected and the sheet header colored to match.

---

## Add / Edit Flow

1. User taps **+ Cash In** or **+ Cash Out**.
2. `AddCashEntrySheet` opens with `type` fixed (the button choice); the user can't toggle it inside the sheet. Header text reads "Cash In" (green) or "Cash Out" (red).
3. Fields (top to bottom):
   - Amount — numeric keyboard, auto-focus, non-zero validation
   - Category — dropdown picker (default "Other")
   - Date — tap to open `DatePickerDialog`, default today
   - Note — optional, single-line
   - Photo row — Camera / Gallery buttons from existing `PhotoPicker`; thumbnail + X after selection
4. **Save** — calls `vm.save()` which:
   - Validates amount > 0
   - Copies photo (if chosen) to app files dir via `ImageUtils.saveImageToAppDir`
   - Calls `repo.addCashEntry(entry, imagePath)`
   - Closes sheet
5. **Edit** — same sheet pre-filled, invoked from `CashEntryDetailScreen`'s Edit button via a `mutableStateOf<CashEntry?>` "entryBeingEdited" on the detail screen; Save calls `repo.updateCashEntry`.

## Delete Flow

- Detail screen → Delete button → `AlertDialog("Delete this entry?")`
- Confirm → `vm.delete()` → calls `repo.deleteCashEntry(entry)`
- Repo impl deletes the DB row and, if `imageLocalPath != null`, `File(path).delete()`
- `navController.navigateUp()`

---

## Dependency Injection

`di/DatabaseModule.kt` additions:

```kotlin
@Provides fun provideCashEntryDao(db: DigiDatabase): CashEntryDao = db.cashEntryDao()
```

`DigiDatabase.kt`:

```kotlin
abstract fun cashEntryDao(): CashEntryDao
```

No repository module changes — `DigiRepositoryImpl` already has `@Inject constructor` and will accept `cashEntryDao: CashEntryDao` as an added parameter.

---

## Permissions

No new permissions. CAMERA and READ_MEDIA_IMAGES are already declared in the manifest for customer-ledger photo receipts and are reused here.

---

## What Phase 2a Does NOT Include

- PDF export of the cash book
- Charts / trends / category breakdown reports
- User-defined categories (management UI)
- Multi-currency conversion (uses the book's currency as-is)
- Auto-sync with customer ledger
- Opening-balance setup screen (treat the first Cash In entry as the opening balance)
- Recurring entries
- Widgets / quick-add from home screen

These are candidates for Phase 2a.1 once 2a is shipped and exercised.

---

## Success Criteria

- Bottom-nav "Cash" tab opens `CashRegisterScreen` (no longer shows ComingSoon).
- User can add a Cash In entry; it appears immediately, and the summary card updates.
- User can add a Cash Out entry with a photo receipt; the photo is visible in the row thumbnail and full-screen on detail.
- Filter chips change the date window; summary and list update accordingly.
- Editing an entry updates both list and summary.
- Deleting an entry removes the row, updates summary, and removes the image file from disk.
- Switching books switches the register (entries scoped to `businessId`).
- App still builds with `./gradlew assembleDebug` — no regressions in Phase 1 screens.
- Room migration 1→2 runs cleanly on an upgraded install (no data loss on the 5 Phase 1 tables).
