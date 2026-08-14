# Avenra Design System & Branding Audit Specification

**Document Version:** 2.0.0  
**Phase:** Phase 1B — Avenra Branding Approved & Integrated  
**Status:** Branding Approved & Production Assets Created  

---

## 1. Brand Audit & Approved Identity

### Visual Identity Baseline
- **Application Brand:** **Avenra** `[APPROVED]`
- **Logo Concept:** **Prism Mark** `[APPROVED]` (Faceted 4-point diamond emblem with central star representing discovery, selection, clarity, and modern e-commerce).
- **Branding Status:** Approved production vector branding created and integrated into Android project resources. All legacy Route branding replaced.

### Approved Brand Colors
- **Primary Brand Blue:** `#004197` `[APPROVED]`
- **Dark Navy (Text & Surface):** `#061023` `[APPROVED]`
- **White (Surface):** `#FFFFFF` `[APPROVED]`
- **Light Background:** `#F8F9FA` `[APPROVED]`
- **Surface Variant:** `#F2F4F7` `[APPROVED]`

### Typography Baseline
- **Font Family:** `Inter` / System Sans-Serif (`FontFamily.Default`) `[APPROVED]`
- **Wordmark Typography:** SemiBold / Medium title-cased `"Avenra"` `[APPROVED]`

---

## 2. Color System

| Token Name | Applied Surface | Color Value | Status |
| --- | --- | --- | --- |
| `primary` / `avenra_primary` | Primary brand accent, primary buttons, status bar | `#004197` | **APPROVED** |
| `onPrimary` / `avenra_white` | Text & icons on `primary` elements | `#FFFFFF` | **APPROVED** |
| `primaryContainer` | Chip selected fill, active item container | `#F2F4F7` | **APPROVED** |
| `onPrimaryContainer` | Text & icons on `primaryContainer` | `#061023` | **APPROVED** |
| `background` / `avenra_background` | Screen background across light mode screens | `#F8F9FA` | **APPROVED** |
| `onBackground` / `avenra_dark_navy` | Primary body text and titles on background | `#061023` | **APPROVED** |
| `surface` / `avenra_white` | Card background, top app bar surface | `#FFFFFF` | **APPROVED** |
| `onSurface` | Main text on surface elements | `#061023` | **APPROVED** |
| `surfaceVariant` / `avenra_surface_variant` | Input field fill, card stroke background | `#F2F4F7` | **APPROVED** |
| `onSurfaceVariant` | Subtitle text, placeholder text, unselected icons | `#667085` | **INFERRED** |
| `outline` | Card borders, text field outlines, divider lines | `#D0D5DD` | **INFERRED** |
| `error` | Form validation error text, error banners, out-of-stock badges | `#D92D20` | **INFERRED** |

*Dark Mode Palette:* **TBD** (Light theme baseline approved; dark theme maps primary blue `#004197` to dark navy surface).

---

## 3. Approved Branding Assets & Resource Locations

| Asset Identifier | Asset Purpose | Resource Location | Status |
| --- | --- | --- | --- |
| `ic_avenra_logo_mark.xml` | Vector Prism Logo Mark (Faceted 3D Blue & White Star) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |
| `ic_avenra_logo_mark_light.xml` | Monochrome Light Logo Mark (for Dark Backgrounds) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |
| `ic_avenra_logo_mark_dark.xml` | Monochrome Dark Logo Mark (for Light Backgrounds) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |
| `ic_avenra_logo_horizontal.xml` | Horizontal Logo Lockup (Prism Mark + "Avenra" Wordmark) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |
| `ic_launcher_background.xml` | Adaptive Launcher Background (`#004197` Primary Blue) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |
| `ic_launcher_foreground.xml` | Adaptive Launcher Foreground (Centered Prism Mark) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |
| `ic_launcher.xml` | Android Adaptive Icon Configuration | `app/src/main/res/mipmap-anydpi-v26/` | **APPROVED & CREATED** |
| `ic_launcher_round.xml` | Android Round Adaptive Icon Configuration | `app/src/main/res/mipmap-anydpi-v26/` | **APPROVED & CREATED** |
| `ic_avenra_splash_background.xml` | Splash Background Layer-list (`#004197` + Light Mark) | `app/src/main/res/drawable/` | **APPROVED & CREATED** |

---

## 4. Spacing System

| Token | Value | Applied Layout Context | Status |
| --- | --- | --- | --- |
| `spacingXSmall` | 4dp | Icon-to-text spacing, tight badge padding | **VERIFIED** |
| `spacingSmall` | 8dp | Item internal padding, chip padding | **VERIFIED** |
| `spacingMedium` | 16dp | **Standard Screen Edge Padding**, card content padding | **VERIFIED** |
| `spacingLarge` | 24dp | Section vertical gap | **VERIFIED** |
| `spacingXLarge` | 32dp | Header top margin | **VERIFIED** |
| `gridGap` | 16dp | Product grid horizontal and vertical gap | **VERIFIED** |

---

## 5. Shape / Radius System

| Surface Component | Corner Radius (dp) | Shape Specification | Status |
| --- | --- | --- | --- |
| Buttons (Primary & Secondary) | 40dp (Pill) | `RoundedCornerShape(40.dp)` | **VERIFIED** |
| Text Fields / Input Search | 8dp | `RoundedCornerShape(8.dp)` | **VERIFIED** |
| Product Cards | 15dp | `RoundedCornerShape(15.dp)` | **VERIFIED** |
| Category Cards / Banners | 15dp | `RoundedCornerShape(15.dp)` | **VERIFIED** |
| Chips / Badges | 20dp (Pill) | `RoundedCornerShape(20.dp)` | **VERIFIED** |

---

## 6. Token Classification Summary

### APPROVED
- Brand Name: `Avenra`
- Brand Emblem: `Prism Mark` (Faceted diamond with concave center star)
- Primary Blue Accent: `#004197`
- Dark Navy Text: `#061023`
- Pure White Surface: `#FFFFFF`
- Light Screen Background: `#F8F9FA`
- Surface Variant Fill: `#F2F4F7`
- Vector Production Assets: `ic_avenra_logo_mark`, `ic_avenra_logo_horizontal`, `ic_launcher`, `ic_avenra_splash_background`

### TBD
- Custom font files (`.ttf` font asset packaging if requested)
- Detailed reusable atomic UI components (Buttons, Cards, Badges)
- Checkout / Payment screen visual layouts
- Full application screen implementation
