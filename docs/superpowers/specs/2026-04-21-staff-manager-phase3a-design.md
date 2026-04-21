# Staff Manager (Phase 3a) — Design Spec

**Date:** 2026-04-21
**Project:** `/Users/macbookpro/Documents/digikhata-clone`
**Depends on:** Phase 2a–2d (Room v5)
**Goal:** Add a per-book Staff Manager — a roster of employees and a salary-payments log — accessed from the drawer.

---

## Overview

Phase 3 was originally "Staff Manager + Firebase Cloud Sync" but those are two independent subsystems. This spec covers **only 3a (Staff Manager)**. Cloud Sync gets its own spec (3b) after 3a ships.

Staff Manager is the last local-only feature module. Each Business has a list of Staff records (name, role, phone, monthly salary, joining date, optional photo/notes). Each staff member has an independent log of StaffPayment entries (amount, date, optional note) — supporting advances and partial payments. A "This month" summary on the detail screen shows monthly salary vs payments this month → balance due. The list row shows the same "due this month" number for quick scanning.

Attendance tracking is **explicitly deferred** to Phase 3a.1 because it opens a large sub-decision tree (daily marks, half-days, leave types) that deserves its own brainstorm.

---

## Decisions (from brainstorming)

| # | Decision |
|---|---|
| Q1 | Split Phase 3 — ship Staff Manager first (3a), Cloud Sync later (3b) |
| Q2 | Staff fields: name, role, phone, monthlySalary, joiningDate, photo?, notes? |
| Q3 | No attendance tracking in 3a (deferred to 3a.1) |
| Q4 | StaffPayment log (amount, date, note) — independent stream |
| Q5 | List row shows "Due Rs X this month" |
| Q6 | Access: drawer entry (bottom nav is full) |
| Q7 | Detail screen: info block + "This month" card + payments list + FAB |

---

## Data Model

### New entity — `Staff`

File: `app/src/main/java/com/digikhata/data/entity/Staff.kt`

```
id: Long              PK, autoGenerate
businessId: Long      FK → businesses.id, indexed, onDelete=CASCADE
name: String
role: String?         nullable
phone: String?        nullable
monthlySalary: Double
joiningDate: Long
imageLocalPath: String?
notes: String?
createdAt: Long
updatedAt: Long
```

Table name: `staff`.

### New entity — `StaffPayment`

File: `app/src/main/java/com/digikhata/data/entity/StaffPayment.kt`

```
id: Long              PK, autoGenerate
staffId: Long         FK → staff.id, indexed, onDelete=CASCADE
amount: Double        always positive
paymentDate: Long     millis
note: String?
createdAt: Long
```

Table name: `staff_payments`.

### DAO — `StaffDao`

File: `app/src/main/java/com/digikhata/data/dao/StaffDao.kt`

- `@Insert suspend fun insert(s: Staff): Long`
- `@Update suspend fun update(s: Staff)`
- `@Delete suspend fun delete(s: Staff)`
- `@Query("SELECT * FROM staff WHERE id = :id") fun getById(id: Long): Flow<Staff?>`
- `@Query("SELECT * FROM staff WHERE businessId = :bid ORDER BY name COLLATE NOCASE ASC") fun getByBusiness(bid: Long): Flow<List<Staff>>`
- `@Query("SELECT COUNT(*) FROM staff WHERE businessId = :bid") fun staffCount(bid: Long): Flow<Int>`
- `@Query("SELECT COALESCE(SUM(monthlySalary),0) FROM staff WHERE businessId = :bid") fun totalMonthlyPayroll(bid: Long): Flow<Double>`

### DAO — `StaffPaymentDao`

File: `app/src/main/java/com/digikhata/data/dao/StaffPaymentDao.kt`

- `@Insert suspend fun insert(p: StaffPayment): Long`
- `@Delete suspend fun delete(p: StaffPayment)`
- `@Query("SELECT * FROM staff_payments WHERE staffId = :sid ORDER BY paymentDate DESC, id DESC") fun getByStaff(sid: Long): Flow<List<StaffPayment>>`
- `@Query("SELECT COALESCE(SUM(amount),0) FROM staff_payments WHERE staffId = :sid AND paymentDate BETWEEN :from AND :to") fun paidBetween(sid: Long, from: Long, to: Long): Flow<Double>`
- `@Query("SELECT COALESCE(SUM(sp.amount),0) FROM staff_payments sp INNER JOIN staff s ON sp.staffId = s.id WHERE s.businessId = :bid AND sp.paymentDate BETWEEN :from AND :to") fun paidThisMonthForBusiness(bid: Long, from: Long, to: Long): Flow<Double>`

### Repository additions — `DigiRepository`

```
fun staffList(businessId: Long): Flow<List<Staff>>
fun getStaff(id: Long): Flow<Staff?>
fun staffCount(businessId: Long): Flow<Int>
fun totalMonthlyPayroll(businessId: Long): Flow<Double>
suspend fun addStaff(staff: Staff, imagePath: String?): Long
suspend fun updateStaff(staff: Staff)
suspend fun deleteStaff(staff: Staff)          // also deletes imageLocalPath; payments cascade

fun staffPayments(staffId: Long): Flow<List<StaffPayment>>
fun paidInRange(staffId: Long, from: Long, to: Long): Flow<Double>
suspend fun addStaffPayment(payment: StaffPayment): Long
suspend fun deleteStaffPayment(payment: StaffPayment)
```

### Migration

`DigiDatabase` — `version = 6`. Entity list grows by two. Add `MIGRATION_5_6` to `data/Migrations.kt`:

```sql
CREATE TABLE IF NOT EXISTS `staff` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `businessId` INTEGER NOT NULL,
  `name` TEXT NOT NULL,
  `role` TEXT,
  `phone` TEXT,
  `monthlySalary` REAL NOT NULL,
  `joiningDate` INTEGER NOT NULL,
  `imageLocalPath` TEXT,
  `notes` TEXT,
  `createdAt` INTEGER NOT NULL,
  `updatedAt` INTEGER NOT NULL,
  FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_staff_businessId` ON `staff` (`businessId`);

CREATE TABLE IF NOT EXISTS `staff_payments` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `staffId` INTEGER NOT NULL,
  `amount` REAL NOT NULL,
  `paymentDate` INTEGER NOT NULL,
  `note` TEXT,
  `createdAt` INTEGER NOT NULL,
  FOREIGN KEY(`staffId`) REFERENCES `staff`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_staff_payments_staffId` ON `staff_payments` (`staffId`);
```

Register via `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)` in `DatabaseModule.provideDatabase`.

---

## Screens & Navigation

### Routing changes

`ui/navigation/Routes.kt`:
- `const val STAFF_LIST = "staffList"`
- `const val STAFF_DETAIL_PATTERN = "staffDetail/{staffId}"`
- `fun staffDetail(id: Long) = "staffDetail/$id"`

`ui/navigation/DigiNavGraph.kt`:
- Add `composable(Routes.STAFF_LIST) { StaffListScreen(navController) }`.
- Add `composable(Routes.STAFF_DETAIL_PATTERN, arguments = listOf(navArgument("staffId") { type = NavType.LongType })) { StaffDetailScreen(navController) }`.
- `showChrome` rule: do NOT add staff routes (they use their own top bar with back button, not the main chrome).
- Wire the drawer's existing "Staff" entry (currently `navController.navigate(Routes.comingSoon("Staff"))` or similar) to `navController.navigate(Routes.STAFF_LIST)`. Check `DrawerContent.kt` for the exact callback.

### Files under `com.digikhata.ui.staff/`

| File | Responsibility | ~lines |
|---|---|---|
| `StaffListViewModel.kt` | @HiltViewModel; `staff`, `staffCount`, `totalPayroll`, `paidThisMonth`, `currency` StateFlows. Computes month range once per open. | 90 |
| `StaffListScreen.kt` | Scaffold + top bar "Staff" (back arrow → popBackStack) + summary strip (count / monthly payroll / paid this month) + LazyColumn + FAB + AddEditStaffSheet state | 150 |
| `StaffRow.kt` | Avatar (40dp, photo or initial), name (titleMedium), role (labelSmall); right column: monthly salary (labelSmall onSurfaceVariant) + "Due Rs X" (titleMedium, green if ≤0 else red) | 100 |
| `AddEditStaffSheet.kt` | ModalBottomSheet: name / role / phone / monthly salary / joining date (DatePicker) / notes / photo / Save | 200 |
| `StaffSheetViewModel.kt` | `suspend fun save(staff, imagePath)` / `suspend fun update(staff)` | 30 |
| `StaffDetailViewModel.kt` | SavedStateHandle → staffId; `staff`, `payments`, `paidThisMonth`, `currency`; suspends for delete, addPayment, deletePayment | 90 |
| `StaffDetailScreen.kt` | Top bar (back + edit + delete); header (photo + name + role); phone row (tappable → dialer); ThisMonthCard; Payments header; list of PaymentRow; FAB "Add Payment"; Edit opens AddEditStaffSheet; Delete confirmation | 200 |
| `ThisMonthCard.kt` | Card with 3 columns: Salary / Paid / Due | 60 |
| `AddPaymentSheet.kt` | ModalBottomSheet: amount (numeric, autofocus) / paymentDate (default today) / note / Save | 130 |
| `PaymentRow.kt` | Date on left; amount right-aligned (DigiRed); long-press triggers AlertDialog → delete | 70 |

### List row layout

```
┌─────────────────────────────────────────────┐
│  (•)  Ahmed Khan                    Rs 30k  │
│       Shop Assistant              Due Rs 15k│
└─────────────────────────────────────────────┘
```

Avatar shows the photo if `imageLocalPath != null`, otherwise a circle with initials on a DigiRed-tinted background.

### "This Month" card on detail

Single `Card`, 3 columns:
```
  Rs 30,000        Rs 15,000          Rs 15,000
  Monthly salary   Paid this month    Due
```
"Due" color: green if ≤0 else DigiRed.

### Add / edit staff flow

1. FAB → `AddEditStaffSheet`.
2. Fields top to bottom: Name (autofocus, required) → Role → Phone (phone keyboard) → Monthly Salary (decimal, required, ≥0) → Joining Date (DatePicker, default today) → Notes (multiline) → Photo row (existing PhotoPicker).
3. Save disabled until name is non-blank and monthlySalary parses.

### Add payment flow

1. FAB on detail → `AddPaymentSheet`.
2. Amount autofocus, date default today, optional note, Save.
3. `repo.addStaffPayment` inserts the row; list + "This Month" card update automatically.

### Delete

- Staff delete: top-bar delete on detail → AlertDialog → `repo.deleteStaff` → `navController.navigateUp()`. Repo deletes image file if present; payments cascade.
- Payment delete: long-press a PaymentRow → AlertDialog → `repo.deleteStaffPayment`.

---

## Dependency Injection

`di/DatabaseModule.kt`:
- `@Provides fun provideStaffDao(db: DigiDatabase) = db.staffDao()`
- `@Provides fun provideStaffPaymentDao(db: DigiDatabase) = db.staffPaymentDao()`
- `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)`

`DigiDatabase.kt`:
- Add `Staff::class`, `StaffPayment::class` to entities
- Bump `version = 6`
- `abstract fun staffDao(): StaffDao`
- `abstract fun staffPaymentDao(): StaffPaymentDao`

`DigiRepositoryImpl` — inject both DAOs; implement the 11 new methods.

---

## Permissions

No new permissions. CAMERA / READ_MEDIA_IMAGES reused for staff photos.

---

## What Phase 3a Does NOT Include

- Attendance tracking (daily marks, half-days, leaves) — deferred to 3a.1
- Shift scheduling
- Overtime / bonus calculations
- Payroll taxes
- Staff login / self-service
- CSV export / reports
- Payslip PDFs

---

## Success Criteria

- Drawer "Staff" entry opens `StaffListScreen` (no longer ComingSoon).
- User can add a staff record with photo; it appears immediately in the list and summary strip updates.
- Tap staff → detail with info, "This Month" card, payments list, FAB.
- Adding a payment updates the list row's "Due this month" and the detail card live.
- Editing a staff record updates fields; editing photo replaces the file.
- Deleting a staff record removes the row, deletes the image file, and cascades payments.
- Long-press a payment → confirm → payment removed, "Due" recomputes.
- Phone row tap opens the phone dialer (standard `Intent.ACTION_DIAL`).
- Switching books switches the staff list.
- App still builds: `./gradlew assembleDebug` → SUCCESS.
- Room migration 5→6 runs cleanly on upgrades.
