# External theme files: an architectural proposal

Status: **proposal, not implemented.** The task explicitly says not to introduce a new theme
format without agreement when it changes the architecture; this document is the investigation and
the design for that decision. Everything here reuses the existing theme engine rather than
replacing it.

## 1. What exists today

* A theme is a `CarThemeSpec` (`ui/theme/CarTheme.kt`): a day `ColorScheme`, a night `ColorScheme`,
  the `CarColors` accents (accept/decline/red/green/blue/yellow), a `Typography`, a `Shapes`, a
  `CarDimens` (touch target, rail width, gutter, icon size), an app-icon shape, and a few switches
  (`singleColor`, `tintIcons`, `glow`, `segmentClock`, `cardBorder`, `dayBackdrop`/`nightBackdrop`).
* The built-in themes are Kotlin objects in `ui/theme/*.kt`, listed by `CarThemes.all`; the
  settings store only `style_theme` (an id), `style_accent` (an accent id, `theme` or `phone`) and
  `style_layout`.
* A theme is applied by `CarTheme(night, theme)` through `MaterialTheme` plus composition locals
  (`LocalCarTheme`, `LocalCarColors`, `LocalCarDimens`, `LocalCarNight`, and the new
  `LocalDockIconStyle`). Every widget, card, rail and dock reads those tokens.
* Settings backup (`SettingsBackup.kt`) exports `style_theme`, `style_layout`, `style_accent`, the
  dock-icon and button settings, the wallpapers and the workspaces. It moves values between phones
  but does not carry a theme definition: an id that isn't built into the target app falls back to
  AARE's theme (`CarThemes.byId`).
* There is **no** theme import/export file format today, and no theme resources outside the APK.

So the hooks are good: a theme is already data (`CarThemeSpec`), and an external file can be
decoded into one without touching any screen. The missing pieces are the file format, its storage
and the picker UI.

## 2. What an external file could carry

Keep the format to what the existing engine can express; anything more would be a new renderer.

| Token group | Examples | Limit |
|---|---|---|
| Colour roles | `background`, `surface`, `surfaceContainer*`, `onSurface`, `onSurfaceVariant`, `primary`, `onPrimary`, `primaryContainer`, `outline`, `error` | ARGB hex, opaque or translucent; both day and night |
| Accent colours | accept, decline, red, green, blue, yellow, ink-on-light | ARGB hex |
| Geometry | touch target, rail width, gutter, icon size, card and app-icon corner radius, card border | dp values with sane bounds |
| Icon treatment | `tintIcons`, the dock's monochrome default, preferred light/dark tone | booleans and tone ids |
| Type | a bundled font file and a size scale, or nothing | a font must be licensed for redistribution; the default is no font |
| Wallpapers/backdrops | a gradient (two or more stops) or a bundled image | images must be packaged in the file and size-limited |
| Identity | id, name, author, version, schema version, preview image, dark/light capability flags | an id is stable; a new file with the same id replaces it |

Not carried: layouts, workspaces, dock-button settings, widget bindings and anything that changes
the phone's data. A theme is appearance, not configuration.

## 3. Proposed file format

A ZIP with a small, versioned JSON manifest and optional images/fonts (a single JSON file is also
accepted when there are no binary assets):

```
mytheme.dgtheme (a ZIP)
├─ theme.json                 manifest, schema "dashgrid-theme/1"
├─ preview.png                optional, ≤ 512 px, shown in the picker
├─ wallpaper-day.jpg          optional, ≤ 1920 px
└─ font.ttf                   optional, with a licence note in theme.json
```

```json
{
  "schema": "dashgrid-theme/1",
  "id": "org.example.midnight",
  "name": "Midnight",
  "author": "…",
  "version": 3,
  "supports": { "day": false, "night": true },
  "colors": {
    "day":   { "background": "#F4F7F9", "onBackground": "#101418", "primary": "#0E7C70", "…": "…" },
    "night": { "background": "#101418", "onBackground": "#F1F5F9", "primary": "#36D6C3", "…": "…" }
  },
  "accents": { "day": { "accept": "#2E7D32", "…": "…" }, "night": { "…": "…" } },
  "dimens": { "touchTarget": 76, "railWidth": 96, "gutter": 8, "iconSize": 34, "cardRadius": 24, "appIconRadius": 24 },
  "icons": { "tint": false, "dockMonochrome": true, "dockToneLight": "dark_grey", "dockToneDark": "white" },
  "font": { "file": "font.ttf", "licence": "OFL-1.1" },
  "backdrop": { "day": ["#FFFFFF", "#EDF2F5"], "night": ["#0B0E12", "#151A20"] }
}
```

Decoding builds a `CarThemeSpec` and a `CarAccent` list, so the rest of the app sees exactly what
it sees for a built-in theme. The format has no executable content, no class names, no resource
ids and no network references: it cannot run code in DashGrid.

## 4. Where it would plug in

1. **Storage:** `filesDir/themes/<id>.dgtheme` or a content URI persisted with
   `takePersistableUriPermission`, plus a validated copy inside `filesDir`. The settings keep a
   small index (`theme_files` = id → file name, display name, version), not a URI that can go
   stale.
2. **Loading:** at startup (and on import) decode each installed file in the background, validate
   it, and add the valid ones to `CarThemes.available` after the built-ins. Invalid files are
   skipped and named in the settings (never crash the car screen); an imported theme that fails
   later falls back to AARE.
3. **Selection:** `style_theme` stays the id field; a file's id cannot collide with a built-in id
   (the importer refuses `dashgrid`, `aare`, `carplay`, … or suffixes it). `CarThemes.byId` returns
   the imported spec when present, the built-in rule otherwise.
4. **Import/export UI:** Settings → Car screen → Appearance gets *Import theme* (SAF
   `OpenDocument`, `.dgtheme`, JSON) and *Export current theme* (saves a manifest built from the
   active `CarThemeSpec`). No automatic download, no update check.
5. **Backup:** `SettingsBackup` keeps exporting the id; it does **not** embed theme files (a file
   can be megabytes). An import whose theme id isn't installed falls back with a notice, as it
   does today.
6. **Dock widgets and app icons:** external themes can set the same icon treatment as built-ins
   (tint/monochrome and tones), but never replace app icons or widget artwork. This keeps the
   "no third-party icon packs" rule.
7. **Wallpapers:** a theme's backdrop is a gradient; bundled images go through the same
   `wallpaper` slot as a user photo, not a new renderer.

## 5. Validation and safety

* Schema version is required and checked; unknown fields are ignored (forward compatibility) but
  an unknown **major** schema is refused.
* Every colour is parsed strictly (`#RRGGBB`/`#AARRGGBB`); every dp is clamped to a documented
  range (for example 24–160 dp touch targets, 8–48 dp radii), so a file cannot make the car screen
  unusable.
* Contrast is checked with the same rule as the built-in theme test
  (`CarThemeContrastTest`): text over its surface must reach the project's minimum. A theme that
  fails is imported but marked "low contrast" and isn't selected silently. The user can still use
  it (their choice), with a warning.
* ZIP entries: no path traversal, size caps (manifest 256 KB, images 8 MB each, total 32 MB),
  no symlinks, images decoded downsampled. Parsing never throws out of the loader.
* Fonts need an explicit licence field; a missing licence imports the theme without the font.
* No JavaScript/HTML/theme expressions: values only. Nothing is fetched at runtime.

## 6. Effort and risk

| Part | Rough size |
|---|---|
| Codec + validation + tests | ~400–600 lines Kotlin + tests |
| `CarThemes` registry and settings index | ~150 lines |
| Import/export UI and file handling | ~250 lines Compose |
| Docs and sample theme | a page + one example |
| Review of contrast and geometry by the maintainer | the real cost |

Risks: a theme file is an untrusted document (validation, as above); the app gains a file picker
and a store of user content; support questions about a theme's contrast are the author's, not
DashGrid's; and a malformed file must never leave the car screen blank. The staged approach is:
(1) codec and read-only import of colour/dimens into a `CarThemeSpec`, (2) picker + export,
(3) optional fonts/images. Each stage ships only after the previous one has been used on a phone.

## 7. Decision requested

Nothing from this proposal has been implemented in 1.1.0. The per-item appearance work in this
release (monochrome dock icons, tones, fixed buttons, Android widgets) uses the existing theme
tokens and settings, so an external theme can set them later without a rewrite. Implementing the
format above needs a maintainer decision on: the ZIP container (vs. a single JSON), whether fonts
and images are in scope, and the contrast policy for imported themes.
