# Phase 3a.1 — Staff Attendance Design

**Status:** approved (autonomous run, user away from computer)
**Date:** 2026-04-22
**Depends on:** Phase 3a (Staff Manager) — shipped

## Goal

Let a shop owner mark daily attendance for each staff member and see how much salary they've earned so far this month based on days present. Accrued earnings inform — but do not replace — the existing "Paid this month / Due" card: this feature shows what the employee is *owed*, the payment card shows what has *actually been paid*.

## Non-Goals

- Automatic payroll run at month end
- Bulk marking (e.g., "every Sunday is WEEK_OFF")
- Attendance export / reports
- Shift times, clock-in/clock-out, GPS check-in

These may come in future phases.

## Data Model

### New Entity — `StaffAttendance`

```kotlin
@Entity(
    tableName = "staff_attendance",
    foreignKeys = [
        ForeignKey(
            entity = Staff::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["staffId", "date"], unique = true),
        Index("staffId")
    ]
)
data class StaffAttendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val date: Long,       // local midnight of the marked day (epoch millis)
    val status: String,   // PRESENT | ABSENT | HALF_DAY | LEAVE | WEEK_OFF
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

Uniqueness on (staffId, date) means one status per day per staff. Re-marking updates the existing row.

### Status Enum

```kotlin
enum class AttendanceStatus(val key: String, val label: String, val fraction: Double) {
    PRESENT("PRESENT", "Present", 1.0),
    ABSENT("ABSENT", "Absent", 0.0),
    HALF_DAY("HALF_DAY", "Half-day", 0.5),
    LEAVE("LEAVE", "Paid leave", 1.0),
    WEEK_OFF("WEEK_OFF", "Week off", 1.0);

    companion object {
        fun fromKey(key: String): AttendanceStatus =
            values().firstOrNull { it.key == key } ?: ABSENT
    }
}
```

### Migration 6 → 7

```kotlin
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `staff_attendance` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `staffId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`staffId`) REFERENCES `staff`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_staff_attendance_staffId_date` ON `staff_attendance` (`staffId`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_attendance_staffId` ON `staff_attendance` (`staffId`)")
    }
}
```

DB version bumps from 6 to 7. Entity added to `DigiDatabase` entities array. DAO registered via `dbModule`.

### DAO — `StaffAttendanceDao`

```kotlin
@Dao
interface StaffAttendanceDao {
    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(staffId: Long, from: Long, to: Long): Flow<List<StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND date = :date LIMIT 1")
    suspend fun find(staffId: Long, date: Long): StaffAttendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: StaffAttendance): Long

    @Query("DELETE FROM staff_attendance WHERE staffId = :staffId AND date = :date")
    suspend fun clear(staffId: Long, date: Long)
}
```

## Domain Logic — `AttendanceCalc`

Pure object, no Android imports. Fully unit-testable.

```kotlin
data class MonthSummary(
    val present: Int,
    val absent: Int,
    val halfDay: Int,
    val leave: Int,
    val weekOff: Int,
    val marked: Int,       // total records in month
    val daysInMonth: Int,
    val earned: Double
)

object AttendanceCalc {
    fun summarize(
        records: List<StaffAttendance>,
        monthlySalary: Double,
        year: Int,
        monthZeroBased: Int
    ): MonthSummary {
        // Filter to records whose date falls inside (year, monthZeroBased)
        // daysInMonth = Calendar.getActualMaximum(DAY_OF_MONTH) for that month
        // perDay = if (daysInMonth == 0) 0.0 else monthlySalary / daysInMonth
        // earned = perDay * sum(AttendanceStatus.fromKey(r.status).fraction)
    }
}
```

Unmarked days count as 0 for earned. This is intentional: the owner marks what happened; missing marks are neither present nor absent yet.

## UI

### Architecture

Attendance lives inside the existing `StaffDetailScreen` route — no new navigation destination. One screen, scrolling content.

Layout (top-down):
1. TopAppBar
2. Staff header (avatar + name + role + phone)
3. **ThisMonthCard** (unchanged: Monthly salary / Paid this month / Due)
4. **AttendanceCalendarCard** ← new
5. Payments list
6. Extended FAB: "Add payment"

### `AttendanceCalendarCard`

- Month header row: `‹` button, centered "April 2026", `›` button
- Legend row: small colored dots labelled P / A / H / L / W (Present / Absent / Half-day / Leave / Week off)
- Summary chip row: `P 18 · A 2 · H 1 · Earned ₨ 11,520` (uses `CurrencyUtils.format`)
- 7-column calendar grid, Monday-first, leading blank cells for alignment, numbered cells 1..daysInMonth
- Each numbered cell: day number + small colored dot if a record exists for that day; today's cell has a ring
- Tap a cell (only if within the active month) → opens `MarkAttendanceSheet` for that staff/date

Colors:
- PRESENT = `DigiGreen`
- ABSENT = `DigiRed`
- HALF_DAY = `#FFB300` (amber)
- LEAVE = `#1E88E5` (blue)
- WEEK_OFF = `MaterialTheme.colorScheme.outline` (grey)

Dot colors live in a private `statusColor(status)` helper inside the card file.

### `AttendanceCalendarViewModel`

- Takes `staffId: Long` via `SavedStateHandle`
- `val visibleMonth: MutableStateFlow<YearMonth>` initialized to current local month
- Derives `(fromMillis, toMillis)` inclusive range for the visible month
- `records: StateFlow<List<StaffAttendance>>` = `dao.observeRange(staffId, from, to).stateIn(...)`
- `summary: StateFlow<MonthSummary>` = `records.combine(staffFlow)` + AttendanceCalc
- `fun prevMonth()`, `fun nextMonth()`, `fun upsert(date, status, notes)`, `fun clear(date)`

Uses `@HiltViewModel` + `@Inject constructor(private val repo: DigiRepository, savedStateHandle: SavedStateHandle)` — same pattern as `ProductDetailViewModel`.

### `MarkAttendanceSheet`

ModalBottomSheet. Receives `(staffId, dateMillis, existing: StaffAttendance?)`.

- Date header: "Wed, 22 Apr 2026"
- 5 status chips (FilterChip row, wrap): Present / Absent / Half-day / Leave / Week off
- Optional notes `OutlinedTextField`
- "Save" (DigiRed filled button)
- "Clear" text button (only shown when `existing != null`), removes the record

### Repository additions

```kotlin
// DigiRepository interface
fun observeAttendance(staffId: Long, from: Long, to: Long): Flow<List<StaffAttendance>>
suspend fun upsertAttendance(record: StaffAttendance): Long
suspend fun clearAttendance(staffId: Long, date: Long)
```

Impl delegates to `StaffAttendanceDao`.

## Date Handling

All "date" values stored are **local midnight** of the marked day. `DateUtils.startOfDay(millis, tz = default)` helper added if not already present. Month range = `[startOfMonth, startOfNextMonth - 1]`.

## Error Handling

- Upsert failures: caught in VM, surfaced as `SharedFlow<String>` error message → snackbar in card scope. Same pattern as `ProductDetailViewModel`.
- Navigating to previous/next month that contains no records is fine (empty flow).
- Staff deleted while viewing: CASCADE removes attendance; VM's staffFlow becomes null, card hides itself.

## Testing

Unit tests (`app/src/test/java/com/digikhata/domain/AttendanceCalcTest.kt`):

1. Empty records → 0 counts, earned = 0
2. All PRESENT for all 30 days of April → earned == monthlySalary (within 0.01)
3. Mixed statuses: 18 P + 2 A + 1 H + 9 unmarked, salary 12000, April → present=18, absent=2, halfDay=1, earned = 12000/30 * (18 + 0.5) = 7400.0
4. WEEK_OFF and LEAVE both contribute full fraction
5. Records from wrong month ignored (e.g., a March 31 record when month = April)
6. February 2025 (28 days) — perDay divisor correct
7. Zero salary → earned = 0 regardless of records

## Implementation Order (for plan)

1. Entity + enum + migration + DB version bump — **build + run existing tests** (should still pass, migration applies)
2. DAO + repository plumbing — no UI yet
3. `AttendanceCalc` + unit tests (TDD: tests first)
4. `AttendanceCalendarViewModel`
5. `MarkAttendanceSheet`
6. `AttendanceCalendarCard`
7. Wire into `StaffDetailScreen`
8. Full build + manual smoke test
9. Commit, push

Each step is a commit.

## Open Questions

None — all design decisions made autonomously per user's standing instruction to proceed on recommendations.
