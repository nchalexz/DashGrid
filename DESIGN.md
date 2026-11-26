# DashGrid design system

Slogan: **Swipe. Arrange. Drive.**

The visual idea is a calm graphite instrument panel with one turquoise signal colour. It is a car
UI first: large targets, short labels, no decoration that moves while driving. DashGrid keeps every
AARE theme and layout; the DashGrid look is a new theme on top, not a redesign of the app.

## Brand tokens

Implemented as `net.prototux.aare.ui.theme.DashGridColors` and applied by `DashGridTheme`
(`ui/theme/CarThemes.kt`). There are no raw hex values in workspace UI code: composables use
Material 3 roles (`primary`, `surfaceContainer`, `onSurfaceVariant`…), which the theme fills from
these tokens.

| Token | Night (default on the car) | Day | Use |
|---|---|---|---|
| Background | `#101418` | `#F4F7F9` | Screen behind workspaces |
| Surface | `#1B2229` | `#E6ECF0` | Cards, widgets, app tiles |
| Surface high | `#252E36` | `#DEE7EC` | Selected item, raised controls |
| Accent | `#36D6C3` | `#0E7C70` | Active dot, map cell, primary actions |
| Text | `#F1F5F9` | `#101418` | Primary labels |
| Text muted | `#94A3B8` | `#47545E` | Hints, inactive dots, placeholders |

Supporting roles: `primaryContainer #0E3B36`, `secondaryContainer #252E36`,
`outlineVariant #2A343D` (grid lines, dividers), `error #FF8A80` (`#B3261E` by day).

Day mode is a real theme, not an inverted night mode: the accent darkens to `#0E7C70` so it keeps
contrast on white, and text becomes `#101418`. The car's day/night signal still chooses between
them; AARE's themes remain selectable in the Style picker and are unchanged.

## Type and shape

- AARE's car type scale (`CarTypography`) is kept: it is designed for ~70 cm viewing distance.
- Workspace UI uses `headlineSmall` for empty states, `titleMedium`/`titleSmall` for names and
  widget titles, `bodyMedium` for hints, `labelLarge`/`labelSmall` for the pager bar.
- Touch targets come from `CarDimens` (76 dp by default, scaled by the control-size setting);
  pager-bar dots have a 36 dp hit area and arrows 48 dp, inside a 64 dp bar.
- Card corner radius comes from the theme (`CarDimens.cardRadius`, 28 dp in AARE's base dimens);
  app tiles use the theme's `appIcon` shape — a 24 dp rounded square in `DashGridTheme`.
- Gaps between grid cells use `LocalCarDimens.gutter` (12 dp at the default scale), so they grow
  with the control-size accessibility setting.

## Icon

Concept: one workspace in a 70/30 layout — a tall turquoise pane on the left and two muted panes
stacked on the right. Three shapes, no frame, no paging dots, no side pages. Graphite background,
turquoise accent, flat geometry, no text, no car, no road, no gradients or glow. It stays readable
at 48 px.

| Asset | File |
|---|---|
| Adaptive foreground | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| Monochrome | `app/src/main/res/drawable/ic_launcher_monochrome.xml` |
| Adaptive background | `app/src/main/res/drawable/ic_launcher_background.xml` |
| Adaptive icon | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` |
| Legacy square PNGs | `app/src/main/res/mipmap-*/ic_launcher.png` (48–192 px) |
| Legacy round PNGs | `app/src/main/res/mipmap-*/ic_launcher_round.png` (48–192 px) |
| Store icon | `fastlane/metadata/android/en-US/images/icon.png` (512 px) |

Safe zone: the mark is 50 × 50 dp on the 108 dp adaptive canvas, and its rounded corners stay
inside the central 66 dp circle, so a round launcher mask never clips it. The legacy PNGs use the
same mark at 64 × 64 dp, where no adaptive mask applies.

## Dock appearance

- **Monochrome is a gradient, not a tint.** The dock's app icons keep their artwork; a ColorMatrix
  removes the hue and maps the luminance into White (light, for dark backgrounds), Grey (plain),
  Dark grey (for light backgrounds) or Black (the darkest that still separates details). Never a
  single-colour fill, never a replaced icon, never an AdaptiveIconDrawable's monochrome layer: the
  same rule for every app, so the dock reads as one set.
- The light theme's default is Dark grey, the dark theme's White. Only the dock's icons follow the
  setting; the app grid and workspaces keep their colours, and the active-app ring is unchanged.
- **Home and App Dash** are system actions, so they use the theme's shapes and colours, not the
  icon treatment: secondary containers, the primary container when their screen is current. They
  stay at the driver's end, outside the scrolling app row.

## Screen composition

- **Wallpaper** fills the content area; workspaces are transparent over it.
- **Grid**: 10 × 6 logical cells, 12 dp gaps, items snap to cells. A card fills its cells; inner
  padding is 16–24 dp depending on the widget.
- **Widgets** keep AARE's layouts: a card is a surface with optional border, title, and one clear
  action. Placeholders use the same card shape and muted text so an empty widget never looks like
  a broken screen.
- **Pager bar**: full-width, 64 dp, a vertical gradient from transparent to the background so it
  reads over a map without a hard edge. Contents centred: name (max 180 dp, ellipsis), dots,
  arrows. Up to eight dots; more becomes “current/total”.
- **App tile**: icon at 42 % of the shorter cell edge (40–120 dp), label only when the cell is big
  enough; both centred.
- **Empty workspace**: icon, “This workspace is empty”, a hint that points at the phone editor, and
  an “Open apps” button. Never a blank screen.

## Motion

- Page changes: `HorizontalPager`'s spring/fling; no fade or scale on the workspace itself (less
  flicker, less motion). The bar updates as the page crosses the midpoint.
- Bar drag animates exactly one page on release; dragging the bar does not move pages live, so a
  map is never resized mid-gesture.
- No looping or attention-seeking animation on the car screen. The existing assistant bar, notices
  and call overlays are unchanged.

## Localization and wording

- English and Russian are the maintained languages for the new strings; other AARE locales fall
  back to English for strings added by DashGrid. Existing translations keep working, with the
  product name replaced.
- UI text is short and verb-first: “Add widget”, “Use as default”, “Swipe anywhere”.
- “Workspace” is the user-facing term everywhere (Russian: «рабочий стол»). Code uses
  `Workspace`; the word “dashboard” is kept for AARE's legacy single-screen dashboard and its
  widget set (`DashCard`).
- Place names, app labels and workspace names come from the user; no truncation with “…” in the
  editor where the full name matters (the car bar may ellipsize).

## What not to do

- Do not add a second navigation paradigm (tabs, bottom sheets, gestures with hidden meaning) next
  to the pager; dots + arrows + swipe is the whole model.
- Do not shrink targets below `CarDimens.touchTarget` to fit more widgets.
- Do not put text on the launcher icon or add a car/road motif.
- Do not change AARE's theme ids or `DashCard` ids: they are stored in settings.
- Do not animate backgrounds, glows or parallax; the driver's attention is the scarce resource.
