# Implementation Plan – Finora Native Android PDF & Invoice Generation System

## 1. Overview & Objective
Build a complete, production-ready **PDF Reports & Invoice** system for the Finora Android Finance application. The system will empower users to:
1. Create, edit, calculate, persist, preview, share, and print professional invoices with custom line items, BDT (৳), discounts, tax/charges, and Bangla/English typography.
2. Generate comprehensive **Income & Expense Statements** with multi-range filters (today, week, month, year, custom) directly from authoritative database records.
3. Generate detailed **Savings & Investment Reports** distinguishing liquid goal deposits from portfolio equity/investments.
4. Generate **Due, Receivable & Payable Statements (পাওনা, দেনা, বাকি)** and customer-wise ledgers with payment histories.
5. Manage previously generated invoices and reports with a dedicated **Report History** manager (view snapshots, share, print, and search).

---

## 2. Architecture & Data Layer Design

### A. Room Database Entities & DAO
We will add dedicated Room persistence for invoices and line items, plus historical PDF tracking, following the existing Room M3/MVVM architecture:
- **`InvoiceEntity`**:
  - `id: Long = 0` (Primary key, autoincrement)
  - `invoiceNumber: String` (Unique, e.g. `INV-2026-0001`)
  - `businessName: String`, `businessPhone: String?`, `businessAddress: String?`
  - `customerName: String`, `customerPhone: String?`, `customerAddress: String?`
  - `invoiceDate: Long`, `dueDate: Long?`
  - `subtotalPaisa: Long`, `discountPaisa: Long`, `additionalChargesPaisa: Long`, `grandTotalPaisa: Long`, `paidAmountPaisa: Long`, `dueAmountPaisa: Long`
  - `paymentMethod: String?` (Cash, bKash, Nagad, Bank, etc.)
  - `notes: String?`, `terms: String?`
  - `status: String` (PAID, PARTIAL, DUE)
  - `language: String` (BN, EN)
  - `createdAt: Long`, `updatedAt: Long`
  - `pdfFilePath: String?`
- **`InvoiceItemEntity`**:
  - `id: Long = 0` (Primary key, autoincrement)
  - `invoiceId: Long` (Foreign key to `InvoiceEntity` with CASCADE delete)
  - `itemName: String`
  - `description: String?`
  - `quantity: Double` (Supports decimal quantities like 2.5 kg or 10 pcs)
  - `unitPricePaisa: Long`
  - `totalPricePaisa: Long`
  - `sortOrder: Int`
- **`PdfReportHistoryEntity`**:
  - `id: Long = 0`
  - `reportType: String` (`INVOICE`, `INCOME_EXPENSE`, `SAVINGS_INVESTMENT`, `DUE_RECEIVABLE_PAYABLE`, `PARTY_STATEMENT`)
  - `title: String`
  - `subtitle: String?`
  - `fileName: String`
  - `filePath: String`
  - `fileSizeBytes: Long`
  - `dateRangeStart: Long?`, `dateRangeEnd: Long?`
  - `createdAt: Long`

### B. Database Migration (Version 7 to 8)
- Implement `MIGRATION_7_8` in `AppDatabase.kt` to safely create `invoices`, `invoice_items`, and `pdf_reports` tables with appropriate foreign keys and indices (`invoice_number`, `invoice_id`, `created_at`).
- Update database version in `@Database` from `7` to `8`.
- Abstract DAO through `InvoiceDao` and `InvoiceRepository`.

---

## 3. PDF Rendering Engine (`PdfReportGenerator.kt` & Canvas Layout)

### A. Core Engine Highlights
- **Engine**: Android standard `android.graphics.pdf.PdfDocument` (A4 standard: 595 x 842 points).
- **Bangla Font Rendering**: Load bundled font assets (`res/font/noto_sans_bengali_regular.ttf` and `semibold.ttf`) using `Typeface` and `android.text.StaticLayout` / `TextPaint` to ensure correct Bengali complex glyph shaping, virama/hasant conjuncts, and vowel matras without square fallback boxes or broken characters.
- **Finora Corporate Palette**:
  - Primary Navy Blue (`#0F172A`), Header Accent (`#1E3A8A`), Finora Blue (`#2563EB`)
  - Income & Receivables: Vibrant Growth Green (`#16A34A` / `#059669`)
  - Expenses & Payables: Alert Red (`#DC2626`)
  - Clean light borders (`#E2E8F0`), alternate row fills (`#F8FAFC`), white card canvas.
- **Automatic Multi-Page Handling & Table Pagination**:
  - Calculate content heights dynamically with row measuring.
  - Automatically insert page breaks and repeat table column headers with matching column widths on pages 2, 3, etc.
  - Running footers on every page: "পৃষ্ঠা X এর Y" (Page X of Y), report timestamp, Finora branding.
- **Watermark & Logo**: Embedded Finora emblem on canvas with subtle 0.05 opacity watermark for print polish.

### B. Five Report Types
1. **Invoice Document**:
   - Organization/Seller block + Customer/Client block.
   - Metadata grid: Invoice #, Date, Due Date, Payment Status badge.
   - Line items table: #, Item & Description, Qty, Unit Price (৳), Total (৳).
   - Summary calculation box: Subtotal, Discount, Charges, Grand Total, Paid, Balance Due.
   - Payment method, Notes & Terms, and Authorized Signature area.
2. **Income & Expense Statement**:
   - Header with active date period (Today, This Week, This Month, This Year, Custom).
   - Summary Metric Cards: Total Income, Total Expenses, Net Profit/Loss.
   - Category Breakdown summary table.
   - Detailed chronologically sorted transaction ledger (Date, Category/Source, Note, Payment Method, Amount).
3. **Savings & Investment Report**:
   - Part 1: Savings Goals summary (Target, Saved, Remaining, Progress %, History).
   - Part 2: Investments & Portfolio (Stock holdings, cost value, market value, unrealized profit/loss, return %).
   - Clear distinction between cash savings and equity/portfolio investments.
4. **Due, Receivable & Payable Statement (পাওনা ও দেনা খাতা)**:
   - Summary totals: Total পাওনা (Receivable), Total দেনা (Payable), Net Position.
   - Customer-wise receivables ledger and Vendor-wise payables ledger.
   - Overdue highlighting and phone contact details.
5. **Person-Wise Account Statement**:
   - Single selected customer/vendor ledger statement over custom date range.
   - Transaction-by-transaction credit/debit audit trail with running balance.

---

## 4. UI/UX Flow & Screen Hierarchy

### Screen: `PdfReportsHubScreen`
- Route: `Screen.PdfReports` with sub-views or clean tabs:
  1. **ইনভয়েস তৈরি ও তালিকা (Invoices & Generator)**:
     - Button: "+ নতুন ইনভয়েস তৈরি করুন" (Create New Invoice).
     - Filterable list of saved invoices (Search by invoice # or customer, filter by status).
     - Actions per invoice: View / PDF, Share, Print, Edit, Delete.
  2. **আর্থিক রিপোর্ট তৈরি (Generate Reports)**:
     - Module cards for:
       - আয়-ব্যয় রিপোর্ট (Income & Expense)
       - সঞ্চয় ও বিনিয়োগ রিপোর্ট (Savings & Investment)
       - দেনা-পাওনা ও বাকির রিপোর্ট (Due, Receivable & Payable)
       - ব্যক্তিভিত্তিক লেজার স্টেটমেন্ট (Person Statement)
     - Filter dialogs: Period selectors (আজ, এই সপ্তাহ, এই মাস, চলতি বছর, নির্দিষ্ট তারিখ / Custom), Party picker.
     - Direct actions: "প্রিভিউ দেখুন" (Preview), "পিডিএফ শেয়ার করুন" (Share), "প্রিন্ট করুন" (Print).
  3. **রিপোর্ট হিস্ট্রি (Report History)**:
     - List of generated PDFs saved in app storage.
     - Quick open, share, print, or delete snapshot.

### Screen: `CreateEditInvoiceScreen` / Dialog
- Comprehensive invoice creation workflow:
  - Auto-generated invoice number based on `OrganizationInfoRepository` prefix.
  - Organization details pre-filled from app preferences (can be adjusted).
  - Customer selection (can pick from existing `LedgerParty` contacts or enter manually).
  - Date & Due Date pickers using `FinoraDatePickerDialog`.
  - Dynamic line items manager (Add item, title, description, quantity, price, delete item).
  - Discount (fixed or percentage), Extra charges, Paid amount fields with real-time recalculation of Grand Total and Due Amount using `BigDecimal` precision.
  - Payment method selector (Cash, bKash, Nagad, Bank).
  - Notes & Terms presets.

### Native PDF Actions:
- **Preview**: Render PDF pages onto Compose UI using `android.graphics.pdf.PdfRenderer` into high-resolution Bitmaps with zoom and page flipping.
- **Share**: Use `FileProvider.getUriForFile()` with `Intent.ACTION_SEND` and `FLAG_GRANT_READ_URI_PERMISSION`.
- **Print**: Standard `android.print.PrintManager` with `PrintDocumentAdapter` for seamless wireless/cloud printing.
- **Save to Device**: Android Storage Access Framework (`ActivityResultContracts.CreateDocument("application/pdf")`).

---

## 5. Navigation & Drawer Integration
- Add `DrawerDestination.PDF_REPORTS` in `FinoraDrawer.kt` with descriptive icon (`Icons.Default.PictureAsPdf` or `ReceiptLong`) and string `nav_pdf_reports` ("পিডিএফ ও ইনভয়েস").
- Add navigation item in `HisabPatiHubScreen` as a primary report generator card.
- Add `Screen.PdfReports` and `Screen.InvoiceCreateEdit` in `FinoraApp.kt`.

---

## 6. Verification & Testing
1. **Robolectric & Unit Tests**:
   - `InvoiceCalculationTest`: Mathematical tests verifying subtotal, discount, charges, paid, due balance, and decimal quantities with `BigDecimal`.
   - `PdfReportGeneratorTest`: Validates PDF generation for empty state, single page, and multi-page overflowing transactions without crash.
   - `BengaliTextPdfTest`: Verifies font loading and non-null Typeface fallback.
2. **Build & Lint Verification**:
   - Run `compile_applet` and `gradle :app:testDebugUnitTest` to guarantee zero regressions.
