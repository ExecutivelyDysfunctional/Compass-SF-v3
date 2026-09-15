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
  - AI-assisted unstructured flyer/text parsing into structured database fields.
  - Manual entry fallback editor for submitting new community resources.
- **Settings & Theming**:
  - 5 Theme Modes: Midnight Ink (default dark), OLED Pure Black (battery saver), Daylight Fog (high-contrast light mode), Warm Sunset (warm amber charcoal), Pacific Marine (oceanic teal).
  - 5 Accent Palettes: Beacon Gold, Golden Gate Rust, Pacific Emerald, Ocean Cyan, Mission Violet.
  - Accessible Font Scaling: Standard (100%), Large (115%), Extra Large (130%).
  - High-contrast mode toggle and Compact listing density mode.
  - App launcher icon preference picker.
- **Data Layer & Persistence**:
  - Offline-first Android Room Database (`compass_sf_database`) persisting Resources, Visits, Tasks, Plans, RmpLocations, AppSettings, and Captures.
  - Automated database seeding from curated SF datasets (`SeedData.kt`).
  - Repository layer exposing reactive Kotlin `StateFlow` streams.

---

## [Next Up]
- **Data Portability (Backup & Restore)**: Add an easy, visible way in Settings to Export Data (Download JSON backup of favorites, personal notes, visit logs, and tasks) and Import Data (Upload JSON restore) so user records are never lost.
- **Location & Distance Sorting**: Optional device location integration to sort open resources by proximity with accurate walking distance indicators.
- **Share Resource**: Android share sheet integration to quickly text or copy address, hours, and notes for a resource to a friend or client.
- **Camera/Photo Capture Ingestion**: Visual document intake for flyers and brochures using the zero-permission Android Photo Picker.

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
- `app/src/main/java/com/example/data/Models.kt` - Room entities, data transfer objects, and JSON converters
- `app/src/main/java/com/example/data/Database.kt` - Room Database definition and ResourceDao interface
- `app/src/main/java/com/example/data/Repository.kt` - CompassRepository data access layer and DB seeding logic
- `app/src/main/java/com/example/data/AiService.kt` - Gemini 3.5 Flash REST client, structured parsing, and offline fallbacks
- `app/src/main/java/com/example/data/SeedData.kt` - Pre-seeded curated SF community resources and EBT restaurant locations
