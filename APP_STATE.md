# Application State: Compass SF

## Project Architecture & Delivery Mode
- **Mode:** Multi-File Mode (Native Android application built with Kotlin, Jetpack Compose, Room Database, and Retrofit)

---

## [Implemented]
- **Now Screen (Today's Navigator Dashboard)**:
  - Time-of-day greeting ("Good morning", "Good afternoon", "Good evening, traveler") with subtitle.
  - Quick Category shortcut pills (Food, Shelter, Hygiene, Connect, EBT/Benefits, Medical) routing to Find tab with preset filter.
  - "My Day Checklist" task summary card with direct one-tap toggle completion.
  - "Open Right Now in SF" real-time spotlight cards dynamically verified against current time and day schedule blocks.
- **Find Screen (Directory Search & Interactive SF Map)**:
  - Real-time text search across resource names, summaries, descriptions, sources, and tags.
  - Multi-axis filtering: Category chips (All, Food, Shelter, Hygiene, Health, Crisis, ID/Docs, Benefits, Legal, Connect), Neighborhood dropdown (Tenderloin, SoMa, Mission, etc.), and Data Source dropdown (ShelterTech SF Service Guide, DataSF, 211 Bay Area).
  - Quick-action toggle filters: Open Now (verified against current time), Favorites Only, and Hidden items.
  - Dual presentation modes: Responsive List view and custom Canvas-based SF Map view with street grid, Market Street arterial marker, and interactive category-coded pins with popup inspection cards.
- **Ask Screen (Navigator AI - Gemini Integration)**:
  - Natural language street navigation and resource consultation powered by Gemini 3.5 Flash via Retrofit.
  - Query input with optional neighborhood filter and "Open now only" toggle constraint.
  - Structured response rendering: conversational guidance summary, numbered logical next steps, and specific database-linked "Navigator Picks" with justification.
  - Built-in offline fallback matcher if API key is unconfigured or network is unavailable.
- **Day Screen (My Day & Checklist Engine)**:
  - Task manager with categorized tags (appointment, errand, document, benefit, housing, health, other).
  - Quick task creation dialog (title, category, priority, notes) and task deletion/completion.
  - Native **Swipe-to-Dismiss Gestures**: Interactive right/left swiping on task rows to quickly complete or delete items with spring physics and color-coded backgrounds.
  - Automated daily plan generation organizing stops, estimated hours, and service notes.
- **EBT Screen (Restaurant Meals Program Guide)**:
  - Comprehensive searchable directory of San Francisco CalFresh EBT hot meal restaurant locations.
  - Cuisine filter (Burgers, Mexican, Halal, Pizza, etc.) and neighborhood filter.
  - Favorites filter, chain vs local restaurant filter, and "Open Now" filter.
  - Educational collapsible banner explaining EBT card coding requirements and restaurant checkout rules.
  - Community submission modal to report new EBT hot-meal accepting vendors.
- **Detail Screen**:
  - Detailed resource overview: Category badge, neighborhood, full address with direct Google Maps intent trigger, phone with dialer intent launch, website link.
  - Live open/closed status badge and complete 7-day schedule matrix.
  - Intake requirements, documents to bring, eligibility criteria, and cost indicators.
  - Spoken language badges and AI street tips.
  - Community verification badge and verification tracking timestamp.
  - "Log a Visit" flow recording outcome (Got help, Turned away, Closed, etc.), wait time, 5-star rating, and feedback notes.
  - Personal private user notes saved directly to Room database.
- **Add Screen (Smart Ingestion & Manual Entry)**:
  - **Camera/Photo Capture Ingestion**: Visual document intake for flyers and brochures using the zero-permission Android Photo Picker.
  - **Pinch-to-Zoom Image Viewer**: Tap any flyer thumbnail to open a full-screen, interactive image viewer with pinch-to-zoom and pan gestures to verify text while parsing data.
  - AI-assisted unstructured flyer/text parsing into structured database fields.
  - Manual entry fallback editor for submitting new community resources.
- **Settings & Theming**:
  - **Quick Settings Index & Jump Navigation**: Fast-jump index grid at the top of the Settings screen with interactive direct-scroll buttons (Atmosphere, Compass Accent, Text Scaling, Street Display, Launcher Icon, Backup & Restore, Hidden Manager, App Statistics), matching numbered section badges on each card, quick "Index ↑" return buttons, and a floating animated "Scroll to Top" button.
  - 5 Theme Modes: Midnight Ink (default dark), OLED Pure Black (battery saver), Daylight Fog (high-contrast light mode), Warm Sunset (warm amber charcoal), Pacific Marine (oceanic teal).
  - 5 Accent Palettes: Beacon Gold, Golden Gate Rust, Pacific Emerald, Ocean Cyan, Mission Violet.
  - Accessible Font Scaling: Standard (100%), Large (115%), Extra Large (130%).
  - High-contrast mode toggle and Compact listing density mode.
  - App launcher icon preference picker.
- **Data Layer & Persistence**:
  - Offline-first Android Room Database (`compass_sf_database`) persisting Resources, Visits, Tasks, Plans, RmpLocations, AppSettings, and Captures.
  - Automated database seeding from curated SF datasets (`SeedData.kt`).
  - Repository layer exposing reactive Kotlin `StateFlow` streams.
- **Data Portability & Backup (Export & Import)**:
  - Settings section with live counter of personal records (Favorites, Private Notes, Visit History Logs, Checklist Tasks).
  - Export Data (Download JSON) via Android document storage picker (`ActivityResultContracts.CreateDocument`).
  - Share Backup via Android share sheet (`Intent.ACTION_SEND`).
  - Import Data (Upload JSON) via Android document file picker (`ActivityResultContracts.OpenDocument`).
  - Direct "Paste Text" backup restore for clipboard/cross-device transfers.
  - Interactive Pre-restore Review dialog summarizing records found with safe merge into existing database without data loss.
  - Real-time success and error banners with itemized summary of records restored.
- **Device Location & Walking Distance Proximity Sorting**:
  - Optional zero-cloud on-device location engine using Android `LocationManager` (GPS / Network) or manual San Francisco neighborhood anchor selector (Tenderloin, Civic Center, SoMa, Mission, Bayview, etc.).
  - 100% local, on-device Haversine distance math—no location coordinates or telemetry are ever sent over network/APIs.
  - Interactive top `LocationBar` on **Now Screen**, **Find Screen**, and **EBT Screen** displaying current location status with one-tap location picker dialog.
  - One-tap "Sort by Distance" toggle dynamically reordering available services, meals, and hot-meal restaurants from nearest to farthest.
  - Visual walking distance badges (e.g. `🚶 0.3 mi` or `🚶 < 250 ft`) displayed across all `ResourceCard` and `RmpCard` components.
- **Mobile Ergonomics & Safeguards (Pixel 8 Pro Standards)**:
  - **Edge-to-Edge Native Viewport**: Active `enableEdgeToEdge()` with system bar safe insets and navigation bar padding preventing UI clipping across gesture and 3-button navigation.
  - **44px / 48dp Minimum Touch Targets**: Full audit across all interactive elements (List/Map switchers, filter toggles, favorite buttons, AssistChips, dropdown triggers, and checklist items) ensuring easy single-handed thumb operation.
  - **Visual Error Handling**: Comprehensive on-screen visual banners and Toasts for all external intent launches (Maps directions, phone dialers, web links), API queries, and JSON import/export operations, completely eliminating silent failures.

---

## [Next Up]
- **Share Resource Card**: Android share sheet integration to quickly text or copy address, hours, and notes for a resource to a friend or client.
- **Resource Verification & Community Wait Time Analytics**: Extended historical wait time graphing and crowdsourced open-now confirmation telemetry.

---

## [Out of Scope]
- Cloud multiplayer or multi-device account sync requiring mandatory logins or external user auth.
- Paid commercial advertisements or sponsored listings.
- Web-based D3/WebView charting (keeping all UI native in Jetpack Compose).

---

## [Files]
- `APP_STATE.md` - Central application state registry and roadmap
- `metadata.json` - Platform metadata and app identity
- `app/build.gradle.kts` - Gradle module configuration and dependencies
- `app/src/main/AndroidManifest.xml` - Android application manifest
- `app/src/main/res/values/strings.xml` - Android localized strings
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` - Adaptive launcher icon
- `app/src/main/res/drawable/ic_launcher_background.xml` - Launcher background drawable
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - Launcher foreground drawable
- `app/src/main/java/com/example/MainActivity.kt` - Main Activity, top bar, FAB, bottom navigation, and NavHost routing
- `app/src/main/java/com/example/ui/Screens.kt` - Compose screens (Now, Find, Ask, Day, Ebt, Add, Info, Settings, Detail) and Map View
- `app/src/main/java/com/example/ui/Navigation.kt` - Screen sealed class definitions and CompassViewModel state management
- `app/src/main/java/com/example/ui/Theme.kt` - Custom M3 theming system, theme modes, accent palettes, and typography scaling
- `app/src/main/java/com/example/data/LocationHelper.kt` - 100% on-device Haversine proximity calculations and SF neighborhood anchor resolution
- `app/src/main/java/com/example/data/Models.kt` - Room entities, data transfer objects, and JSON converters
- `app/src/main/java/com/example/data/Database.kt` - Room Database definition and ResourceDao interface
- `app/src/main/java/com/example/data/Repository.kt` - CompassRepository data access layer and DB seeding logic
- `app/src/main/java/com/example/data/AiService.kt` - Gemini 3.5 Flash REST client, structured parsing, and offline fallbacks
- `app/src/main/java/com/example/data/SeedData.kt` - Pre-seeded curated SF community resources and EBT restaurant locations
