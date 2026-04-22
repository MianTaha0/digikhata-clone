# DigiKhata Clone — Roadmap

**Last updated:** 2026-04-22

Everything remaining, grouped by phase with dependencies and rough effort. Each phase ships independently, follows the **spec → plan → build → test → commit → push** cadence, and stays local-first unless marked otherwise.

Legend: 🟢 small (1-2 days) · 🟡 medium (3-5 days) · 🔴 large (1-2 weeks+) · ☁️ requires network · 💳 paid service

---

## ✅ Shipped

- Phase 1: Onboarding, books, home
- Phase 2a: Clients, transactions, supplier tab
- Phase 2b: Cash register, expenses
- Phase 2c: Invoicing (+ PDF)
- Phase 2d: Inventory / stock
- Phase 3a: Staff manager
- Phase 3a.1: Attendance
- Housekeeping: deprecated-API sweep, settings scaffold, 31 unit tests

---

## Phase 3b — Cloud Sync 🔴 ☁️ 💳

**Why first:** Unlocks multi-device, phone-OTP login, and all future "share / collaborate" features. Everything else is more valuable once data lives off-device.

### 3b.1 — Firebase Auth (phone OTP)
- Firebase project + `google-services.json`
- Phone-number + SMS OTP flow
- Account entity locally, `userId` stamped on every future row
- Sign-out wipes or keeps local data (user choice)

### 3b.2 — Firestore schema + one-way push
- Schema: `users/{uid}/businesses/{bid}/{clients,transactions,...}`
- Repo gains `pushToCloud()` — runs on save + periodic WorkManager job
- Per-row `serverUpdatedAt`, soft-delete via `deletedAt`

### 3b.3 — Two-way sync + conflict resolution
- Pull deltas on app open + every 15 min
- Last-writer-wins by `updatedAt` (simple, matches DigiKhata behavior)
- Offline queue (Room `sync_ops` table) retried when online

### 3b.4 — Account UI
- "Sign in" button replaces Settings → "Cloud Sync: Soon"
- Sync status pill in drawer ("Synced 2 min ago" / "Offline")
- Sign-out, delete-account flows

**Notes:** Firebase free tier handles 50K reads/day — fine for personal use. No backend code needed.

---

## Phase 4a — Reminders & Collections 🟡 ☁️

The feature Indian shop owners use DigiKhata for most.

### 4a.1 — SMS/WhatsApp due reminders
- "Send reminder" button on any client with outstanding balance
- Opens SMS or WhatsApp intent pre-filled with: `{clientName}, your balance is ₨{amount}. Pay at: {link}`
- Templates editable in Settings

### 4a.2 — UPI / QR collection links
- Business profile gains UPI ID (`abc@paytm`) or QR image
- Generated payment link: `upi://pay?pa={vpa}&pn={biz}&am={amt}&tn={clientName}`
- Link embedded in reminders
- No real payment integration needed (UPI apps handle it)

### 4a.3 — Automatic due-date notifications
- Local notifications on invoice due date
- Daily background check via WorkManager
- Tap → opens client detail

### 4a.4 — Recurring entries
- Rent, salary, subscriptions
- `RecurringRule` entity (cadence, next-fire date, template)
- WorkManager fires → posts entry → advances next-fire

---

## Phase 4b — Reports & Exports 🟡

### 4b.1 — Ledger PDF per client
- Date range picker → PDF with transactions table + running balance + logo
- Reuse `InvoicePdfGenerator` infrastructure
- Share intent (email / WhatsApp / Drive)

### 4b.2 — CSV/Excel export
- Per-module: clients, transactions, cash, expenses, invoices, inventory
- CSV via `androidx.documentfile`
- Written to user-picked directory

### 4b.3 — Reports dashboard
- New bottom-nav tab or drawer entry: **Reports**
- Cards: sales this month, expenses this month, cash-in-hand, top 5 clients by balance
- Simple line chart (last 30 days revenue) — use `Canvas` or a lightweight chart lib

### 4b.4 — Profit & Loss
- P&L statement combining: invoice revenue − inventory cost of goods − expenses − salaries
- Date range → single page summary + PDF export

### 4b.5 — Restore from backup
- Counterpart to 4b.2 CSV export
- Picks ZIP from Files → restores into active book

---

## Phase 4c — Localization 🟡

### 4c.1 — String resource extraction
- Move every hardcoded UI string to `res/values/strings.xml`
- Script-assisted sweep (~600 strings estimated)

### 4c.2 — Language packs
- Hindi (`values-hi`), Urdu (`values-ur`), Arabic (`values-ar`), Bengali (`values-bn`)
- Use machine translation as first pass; manual pass for business/accounting terms
- Currency + date formatting per locale

### 4c.3 — Language picker
- Settings → Language → radio list
- Persists via DataStore
- `AppCompatDelegate.setApplicationLocales()` applies without restart

---

## Phase 4d — AI Quick Entry 🟡

### 4d.1 — Receipt OCR → Expense
- Camera button on Expense screen
- ML Kit Text Recognition on-device (no network)
- Heuristic parser: total, date, vendor name
- User reviews pre-filled sheet before save
- Original photo attached

### 4d.2 — Voice → Transaction
- Mic button on Home FAB
- Android `SpeechRecognizer` (on-device on API 31+)
- Rule-based parser for English first: amount + client-name fuzzy match + credit/debit keywords
- Confirmation sheet

### 4d.3 — Invoice photo → Purchase invoice
- Harder: extract line items with qty + price
- Heuristic often fails; consider optional Gemini Flash fallback (☁️ 💳)
- Flag as "beta" in UI

### 4d.4 — Multi-language voice
- Hindi/Urdu/English auto-detect once 4c is in
- LLM-backed parser (Gemini Flash) because rule-based doesn't generalize

---

## Phase 5 — Retail Hardware & Security 🟡

### 5.1 — PIN / biometric lock
- 4-digit PIN on app open
- Biometric prompt if enrolled (`BiometricPrompt`)
- Per-device, not per-account

### 5.2 — Barcode scanner for inventory
- ML Kit Barcode Scanning
- Scan → match existing SKU or create new Product pre-filled with barcode
- On the Invoice line-item row too: scan to add

### 5.3 — Thermal printer support (Bluetooth ESC/POS)
- Pair via system Bluetooth settings
- ESC/POS command builder for 58mm / 80mm rolls
- Print: invoice, receipt, ledger summary
- Library: `com.github.DantSu:ESCPOS-ThermalPrinter-Android`

### 5.4 — Dark mode + theme picker
- Material3 dynamic color on Android 12+
- Manual light/dark/system toggle in Settings
- Wired through `DigiTheme` (mostly just flipping `isSystemInDarkTheme()`)

---

## Phase 6 — Growth Polish 🟡

### 6.1 — Customer-facing shared ledger link
- "Share ledger" generates a signed URL (needs 3b Firebase)
- Read-only web view hosted on Firebase Hosting
- Customer sees their transactions, no login needed

### 6.2 — Contacts import
- Permission → read contacts → bulk-add as clients
- Dedupe by phone number against existing clients

### 6.3 — In-app onboarding coach marks
- 3-step tour on first launch: add client → add transaction → send reminder
- Library: `com.github.smart-fun:TapTargetView` or roll our own

### 6.4 — Rate-app + Play Store listing
- `ReviewManager` API in-app review prompt after 7 days or 20 entries
- Actual Play Store listing when we're ready to distribute

### 6.5 — Monetization scaffolding (optional)
- Freemium gating: staff count > 3, invoice count > 50/mo → "Upgrade" screen
- Google Play Billing integration if pursuing
- All gating logic centralized in a `PremiumFeatures` object so it's easy to flip off for development

---

## Phase 7 — Attendance & Payroll Enhancements 🟢

### 7.1 — Bulk attendance marking
- "Mark every Sunday as Week Off" for a date range
- "Copy previous month's attendance template"

### 7.2 — Monthly payroll auto-post
- End-of-month → calculate earned per staff → post debit to Cash register + credit to StaffPayment in one transaction
- Review before posting

### 7.3 — Attendance reports
- Month-wise summary per staff, across all staff
- PDF export

---

## Cross-cutting / continuous

### CI & Quality 🟢
- GitHub Actions workflow: on PR → `./gradlew assembleDebug test lint`
- Danger file for conventional-commit enforcement
- Screenshot tests for key screens (Paparazzi)

### Codebase hygiene 🟢
- Splitting oversized files (`StaffDetailScreen`, `CreateEditInvoiceScreen` are ~400 lines)
- Migration to Kotlin 2.0 Compose compiler plugin (2025 deprecation of the old kotlinCompilerExtensionVersion)
- Gradle version-catalog consolidation (`libs.versions.toml`)

### Distribution
- Signed release APK + upload key management
- Play Store listing + screenshots + store description
- Internal testing track before prod

---

## Recommended execution order

1. **Phase 3b** (Cloud Sync) — foundational, unlocks everything account-based
2. **Phase 4a** (Reminders & Collections) — biggest feel-like-DigiKhata win
3. **Phase 4b** (Reports & Exports) — most-requested once data exists
4. **Phase 4d.1** (Receipt OCR) — small, high-visibility win; can slot in any time since it's local-only
5. **Phase 4c** (Localization) — do after UI has stabilized to avoid re-extracting
6. **Phase 5** (Retail hardware + security) — PIN lock first (quick), then barcode, then thermal printer
7. **Phase 4d.2–4** (Voice + AI polish) — requires 4c's locale groundwork
8. **Phase 6** (Growth polish) — post-MVP, some items need 3b
9. **Phase 7** (Attendance enhancements) — layered on top of 3a.1 when needed

**Rough total effort to match DigiKhata feature-for-feature:** ~8–12 weeks of focused single-developer work, most of it in Phases 3b and 4a–4c.

**Realistic MVP-that-feels-complete:** Ship 3b + 4a + 4b.1 + 4d.1. ~3–4 weeks. Everything else is refinement.

---

## How we track progress

- Each phase gets a spec in `docs/superpowers/specs/YYYY-MM-DD-<phase>-design.md`
- Each phase gets one or more commits with a clear message (never "wip")
- This ROADMAP.md is updated at the top of each phase to check items off
- Unit tests added for every pure domain function (`*Calc`, `*Filter`, `*Parser`)
