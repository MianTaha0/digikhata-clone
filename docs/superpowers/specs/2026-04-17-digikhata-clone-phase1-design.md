# DigiKhata Clone — Phase 1 Design Spec
**Date:** 2026-04-17  
**Project:** `/Users/macbookpro/Documents/digikhata-clone`  
**Goal:** Build a pixel-faithful Android clone of DigiKhata (v9.4.0) from scratch, starting with the core ledger foundation.

---

## Overview

Build a new Android app in `/Users/macbookpro/Documents/digikhata-clone` that replicates DigiKhata exactly. Phase 1 establishes the full foundation: project scaffold, DigiKhata theme, all core data models, customer/supplier ledger, photo receipts, search/filter, and SMS reminders.

---

## Tech Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | Kotlin | Same as current project, proven |
| UI | Jetpack Compose + Material3 | Modern, declarative |
| DI | Hilt | Same as current project |
| DB | Room | SQLite ORM, same as current |
| Navigation | Compose NavHost | Single-activity multi-screen |
| Image picking | ActivityResultContracts | No extra lib needed |
| Camera | CameraX (camerax-core, camerax-camera2, camerax-lifecycle, camerax-view) | Used by DigiKhata itself |
| Build | Gradle 8.7 + AGP 8.2.2 + Kotlin 1.9.22 | Proven compatible |

---

## Color Scheme — DigiKhata Exact

```
Primary:          #E74425  (DigiKhata red)
PrimaryDark:      #C73510  (darker red, status bar)
OnPrimary:        #FFFFFF
Secondary:        #2E7D32  (green — "got" / received money)
OnSecondary:      #FFFFFF
Error:            #B00020
Background:       #F5F5F5  (light grey page bg)
Surface:          #FFFFFF
OnSurface:        #212121
OnSurfaceVariant: #757575
Outline:          #E0E0E0
```

Typography: **Roboto** (system default, matches DigiKhata exactly — no custom fonts needed).

---

## Project Structure

```
app/src/main/java/com/digikhata/
├── data/
│   ├── dao/          (one DAO per entity)
│   ├── entity/       (Room entities)
│   └── repository/   (impl classes)
├── domain/
│   ├── model/        (domain models, e.g. ClientBalance)
│   └── repository/   (interfaces)
├── ui/
│   ├── theme/        (Color.kt, Type.kt, Theme.kt)
│   ├── components/   (shared composables)
│   ├── home/         (customer list + detail)
│   ├── supplier/     (supplier list + detail)
│   ├── book/         (create/edit book)
│   └── navigation/   (NavGraph, Routes)
├── util/
│   (CurrencyUtils, PdfGenerator, etc.)
├── MainActivity.kt
└── DigiKhataApp.kt   (Hilt application class)
```

---

## Data Models (Phase 1 entities)

### Business (= LedgerBook)
```
id, name, ownerName, phone, currency (default "Pakistan Rupee-Rs"),
colorHex, address, city, type, category, tagline,
logoLocalPath, createdAt, updatedAt
```

### Client (customers AND suppliers)
```
id, businessId, type (0=customer, 1=supplier),
name, phone, phone2, cnic, address,
creditLimit, rating, isPinned, isArchived,
createdAt, updatedAt
```

### Transaction
```
id, clientId, businessId,
amount (always positive Double),
type (0 = "You Gave" / debit / customer owes more,
      1 = "You Got" / credit / customer owes less),
notes, entryDate (timestamp millis),
imageLocalPath (nullable), imagesCount (default 0),
smsStatus (-1=none, 0=pending, 1=sent),
createdAt, updatedAt
```
Balance = sum(amount where type=0) - sum(amount where type=1).
Positive balance = customer owes you. Negative = you owe customer.

### TransactionImage
```
id, transactionId, localPath, createdAt
```

### Notification (payment reminders)
```
id, clientName, clientPhone, amount, balance,
currency, details, ledgerLink,
isSeen, isPost, type, entryDate, createdAt
```

---

## Screens — Phase 1

### 1. Splash / Onboarding
- DigiKhata-style splash with red background, white logo
- First launch: create first Business (book)

### 2. Home Screen (Customer Ledger)
**Top bar:** Red, "DigiKhata" title, search icon, notification bell  
**Drawer:** Business list (same as current), + Create New, Share App, Rate App, Settings  
**Body:** List of customers, each card showing:
  - Colored avatar (initials), name, phone
  - Balance (green = you will get, red = you will give)  
**FAB:** Red "+" to add customer  
**Bottom bar:** Home | Cash | Stock | Bills | Expense (icons, red active tint)  
**Search:** Full-screen search overlay, searches by name/phone

### 3. Customer Detail Screen
**Top bar:** Red, customer name, back arrow, WhatsApp icon, reminder bell  
**Balance card:** Red card showing total balance  
**Transaction list:** Dated entries, green (got) / red (gave) accent bars  
**Bottom buttons:** "+ You Gave" (red) | "+ You Got" (green)  
**Long press:** Edit / Delete transaction  
**Each transaction:** Tap to see note, receipt photo if attached

### 4. Add Transaction Bottom Sheet
- Amount input (numeric)
- Date picker (default today)
- Note field (optional)
- Photo attach button (camera or gallery)
- Confirm button

### 5. Transaction Detail / Edit Screen
- View full transaction with receipt photo (zoomable)
- Edit amount, note, date
- Delete option

### 6. Supplier List Screen
- Same layout as Customer List
- Separate tab/section, type=1 clients
- "You will pay" / "You will receive" terminology

### 7. Supplier Detail Screen
- Same as Customer Detail with supplier-appropriate labels

### 8. Add/Edit Customer Bottom Sheet
- Name, phone, phone2 (optional), CNIC (optional), credit limit
- Import from contacts button (READ_CONTACTS permission)
- Save

### 9. Create / Edit Book Screen
- Book name, owner name, currency picker, color picker
- Full-screen from drawer "Create New"

### 10. Book Settings Screen
- Matches DigiKhata: Book Name (edit), Currency (navigate to picker), Theme color
- Share / Invite section

### 11. Search Screen
- Full-screen, search bar auto-focused
- Live results for customers and suppliers
- Tap → opens detail

### 12. Notifications / Reminders Screen
- Bell icon in top bar → this screen
- List of scheduled payment reminders
- Mark as seen, resend via WhatsApp/SMS

---

## Navigation

```
NavHost root:
  ├── home (CustomerList)           ← start destination
  ├── customerDetail/{clientId}
  ├── supplierList
  ├── supplierDetail/{clientId}
  ├── addTransaction/{clientId}     ← bottom sheet
  ├── transactionDetail/{txId}
  ├── search
  ├── notifications
  ├── createBook
  └── bookSettings/{bookId}

Bottom nav tabs: Home | Cash* | Stock* | Bills* | Expense*
  (* = Phase 2, show "Coming Soon" placeholder for now)
```

---

## Permissions (Phase 1)

```xml
READ_CONTACTS      — import customer from phone
CAMERA             — take receipt photo
READ_EXTERNAL_STORAGE / READ_MEDIA_IMAGES  — pick from gallery
USE_BIOMETRIC      — app lock (future)
INTERNET           — future cloud sync
```

---

## Key Interactions

**Give / Got flow:**
1. Customer detail → tap "+ You Gave" or "+ You Got"
2. Bottom sheet slides up with amount field
3. Optional: add note, attach photo (camera or gallery)
4. Tap Save → transaction added, balance updates, sheet closes

**Photo receipt:**
1. In add-transaction sheet, tap photo icon
2. Bottom sheet: "Camera" or "Gallery"
3. Selected image stored in app's private files dir
4. Thumbnail shown on transaction card in detail screen
5. Tap thumbnail → full-screen zoomable view

**SMS Reminder:**
1. In customer detail top bar, tap bell icon
2. Bottom sheet: pre-filled amount, editable message
3. "Send via SMS" or "Send via WhatsApp"
4. Reminder saved to Notifications table

**Search:**
1. Tap search icon in top bar
2. Full-screen overlay, auto-focused keyboard
3. Query filters clients by name or phone (LIKE %query%)

---

## What Phase 1 Does NOT Include

- Cash Register (Phase 2)
- Invoicing / Bills (Phase 2)
- Inventory / Stock (Phase 2)
- Expense Tracker (Phase 2)
- Staff / Attendance / Salary (Phase 3)
- Firebase cloud sync (Phase 3)
- In-app purchases / Premium (Phase 3)

These show as bottom nav tabs with a "Coming Soon" placeholder screen.

---

## File Output

New standalone project at `/Users/macbookpro/Documents/digikhata-clone/`.  
Does NOT modify the existing Kharcha Book app at `/Users/macbookpro/Documents/khata/`.

---

## Success Criteria

- App builds and runs on Android emulator (API 26+)
- Customer and supplier ledger fully functional (add, view, edit, delete)
- Photo receipts attach and display correctly
- Search filters customers/suppliers in real time
- SMS/WhatsApp reminder sends with correct message
- UI matches DigiKhata's red `#E74425` color scheme throughout
- Navigation drawer shows books, switches active book
