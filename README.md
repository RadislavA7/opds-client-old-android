# OPDS Client for Onyx Boox (and other old Android devices)

**Language: [English](#) | [Русский](README.ru.md)**

A lightweight client to browse and download books from **OPDS catalogs**.
Built specifically for **Android 2.3 (API 9, Gingerbread)** and E-Ink devices
(tested on **Onyx Boox Newton i63**), but works on any Android 2.3+.

APK size — about **60 KB**.

---

## Screenshots

*No screenshots available yet.*

---

## Minimum requirements

- **Android 2.3.1 (API 9)** or newer
- ARM architecture (armeabi / armeabi-v7a)
- **~2 MB** of free device storage
- **Wi-Fi / internet** access to reach catalogs
- Permissions requested by the app:
  - `INTERNET` — network requests to OPDS catalogs
  - `WRITE_EXTERNAL_STORAGE` — saving downloaded books

> Note: the app works over **HTTP**. Old devices (Android 2.3) do not support
> modern TLS 1.2 for HTTPS, so the OPDS catalog must be reachable via `http://…`.

> **Root access is NOT required.** The app is a normal Android application and
> works without root. (Root may be only relevant if some very restricted
> device firmware requires it for sideloading apps, which is not specific to
> this app.)

---

## Installation

1. Download the `opds-client.apk` file.
2. Allow installation from unknown sources (Settings → Security).
3. Open the APK with a file manager and confirm installation.
   (Or install via adb: `adb install opds-client.apk`.)
4. Launch the app and add your first catalog.

---

## Features

### Catalog management
- **Multiple OPDS catalogs** at once.
- Add / edit / delete catalogs.
- **HTTP Basic auth** support (login and password) for protected catalogs.
- **Search within a catalog** via the OPDS OpenSearch standard
  (input field + "Find" button).
- Navigate into sub-catalogs directly from the list.

### Downloading books
- Download books into any convenient folder on the device.
- **Choose a folder via a built-in file picker** ("Browse…") or type it manually.
- **Format selection**: if a book has several download links
  (EPUB / MOBI / FB2 / PDF etc.), a list of formats appears. If there is only
  one format, the confirmation dialog shows right away. The format is
  determined by the MIME type the catalog returns.

### Book list
- Shows **title, author, and description** (annotation) per book.
- Icons next to the author and description for clarity.
- Downloaded books live in the chosen folder and open in the installed
  reader on tap.

### Books screen
- Dedicated "Books" screen listing downloaded files.
- Automatic MIME-type detection by file extension
  (fb2, epub, mobi, azw, pdf, txt, html) and open in a suitable reader.

---

## Usage

1. **Add a catalog** — on the main screen press "Add catalog", set a name and
   the OPDS URL. Add login/password if required.
2. **Open a catalog** — tap it in the list.
3. **Search** — type a title/author in the top field and press "Find".
4. **Download a book** — tap the book, confirm the folder (and pick a format
   if there are several).
5. **Download folder** — "Settings" → "Browse…", select or create a folder.

---

## Building from source

Pure Android project (no libraries or dependencies).

Requirements:
- JDK 8
- Android SDK (build-tools, platform android-9 / android-10)

Manual build:
```
aapt package -f -m -J gen -M AndroidManifest.xml -S res -I android.jar
javac -source 1.6 -target 1.6 -bootclasspath android.jar -classpath gen -d classes src/ru/opds/client/*.java
d8 --min-api 9 --output dexout classes
# package resources + classes.dex into APK, zipalign, sign with v1 (jarsigner SHA1withRSA)
```

> For old devices, a **v1 (JAR) signature is required** — Android 2.3 does not
> recognize v2 signatures.

---

## File structure

| Module | Purpose |
|---|---|
| `MainActivity` | Main screen: list of catalogs |
| `SettingsActivity` | Add/edit a catalog (URL, login, password) |
| `CatalogActivity` | Catalog view, search, download |
| `FolderPickerActivity` | Built-in folder picker |
| `AppSettingsActivity` | Settings (download folder) |
| `BooksActivity` | Downloaded books screen |
| `OpdsParser` | Atom/OPDS feed parsing |
| `HttpOp` | HTTP requests with Basic auth |
| `EntryAdapter` | Book list adapter (title, author, description) |
| `CatalogStore` / `AppSettingsStore` | Settings persistence (SharedPreferences) |

---

## License

Distributed freely. Use at your own risk.