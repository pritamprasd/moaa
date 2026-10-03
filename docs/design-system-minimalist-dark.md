# Plain Minimalist Dark Mode Design System
**Mother of All Apps (MOAA) Unified UI Specification**
*Applies to Native Jetpack Compose Tools and LLM Dynamic Web Tools (HTML/JS/CSS)*

---

## 1. Synthesis of Top 10 Minimalist Android & Developer UI Design Guides

To create a cohesive, ultra-refined minimalist developer utility suite, we synthesized principles from the top 10 design frameworks:

| # | Design System / Guide | Core Learned Principle | Application to MOAA |
|---|---|---|---|
| 1 | **Material Design 3 (M3) Dark** | Elevation through surface lightness, not drop shadows. Desaturated accents. | Replace noisy drop shadows with stepped matte surface tones (`#090A0E`, `#121319`, `#181A22`). |
| 2 | **Linear Design System** | High density, razor-sharp 1px dividers, typographic hierarchy over excessive ornamentation. | Remove rainbow/liquid borders and multi-colored glowing cards; adopt crisp 1px borders (`#222531`). |
| 3 | **Nothing OS Design Philosophy** | Monochrome base palette where color is an "event" reserved only for status/telemetry. | 90% monochrome text & icons; color strictly denotes live status (e.g., active green dot, error red). |
| 4 | **Vercel Geist Design System** | Monospace typography for machine data, strict geometric spacing, contrast-driven readability. | Jetpack Compose Monospace & CSS `ui-monospace` for IP addresses, MAC addresses, hashes, latencies. |
| 5 | **Dieter Rams' 10 Principles of Good Design** | "Less, but better" (Weniger, aber besser); unobtrusive, honest, durable. | Eliminate decorative embellishments; containers serve strictly as structured data vessels. |
| 6 | **GitHub Primer Dark Mode** | Semantic token architecture (`canvas-default`, `border-muted`, `fg-subtle`). | Unified naming across Kotlin Jetpack Compose and LLM HTML/JS/CSS apps. |
| 7 | **Raycast Extension Design Guidelines** | Fast, dense, keyboard/tap-first list/grid utilities, unified search & filter affordances. | Minimalist segmented buttons (e.g. `1s | 3s | 5s | 10s`), uniform search bar, standardized action pills. |
| 8 | **Apple HIG Developer Utility Dark** | Neutral dark backgrounds with high-contrast text and subtle pill highlights for active states. | Crisp pill toggles, clean monochrome outlines, and unified corner radii. |
| 9 | **Radix Colors Dark Scale** | 12-step perceptual luminance scale for dark themes ensuring WCAG AAA accessibility. | High-contrast text (`#F4F4F6`), subtle borders (`#222531`), and muted non-vibrating accents. |
| 10 | **Android Modern App Architecture Design** | Predictable gestures, accessible touch targets (≥48dp), edge-to-edge support. | Maintain large tap targets while keeping visual density sleek and compact. |

---

## 2. Core Color Tokens & The 5 Curated Accent Palettes

### 2.1 Base Surface Hierarchy (Neutral Scale)
- **Background Root (`bg-root`)**: `#090A0E` (Deep matte obsidian, zero glare, battery-efficient)
- **Surface Base (`surface-base`)**: `#121319` (Cards, tile containers, list row items)
- **Surface Elevated (`surface-elevated`)**: `#181A22` (Modals, popups, drawer contents, bottom sheets)
- **Surface Active (`surface-active`)**: `#202330` (Active buttons, selected pill segments, hover/press states)
- **Border Default (`border-default`)**: `#222531` (1px clean outline, no rainbow gradients)
- **Border Subtle (`border-subtle`)**: `#181B24` (Subtle dividers, list item separators)

### 2.2 Typography Hierarchy
- **Text Primary (`text-primary`)**: `#F4F4F6` (Pure readable white for headings, values, and titles)
- **Text Secondary (`text-secondary`)**: `#9CA3AF` (Muted neutral gray for labels, subtitles, descriptions)
- **Text Muted (`text-muted`)**: `#64748B` (Deep muted gray for timestamps, placeholders, versions)

### 2.3 The 5 Curated Minimalist Accent Combinations (Selectable in Settings)

Users can select their preferred accent theme in the Settings tab. All 5 palettes are mathematically tuned against the `#090A0E` obsidian background for optimal readability and minimal eye strain:

| Palette ID | Name | Primary Accent | Secondary Accent | Tertiary Accent | Border Tint | Vibe / Rationale |
|---|---|---|---|---|---|---|
| `linear-cyan` *(Default)* | **Linear Ice** | `#38BDF8` (Ice Blue) | `#818CF8` (Soft Indigo) | `#34D399` (Mint) | `#1E293B` | Clean, modern developer utility feel inspired by Linear & VS Code. |
| `nothing-amber` | **Industrial Amber** | `#F59E0B` (Amber) | `#F4F4F6` (Cold White) | `#E2E8F0` (Slate) | `#262626` | High-contrast utilitarian look inspired by Nothing OS & Teenage Engineering. |
| `nordic-emerald` | **Nordic Emerald** | `#10B981` (Emerald) | `#06B6D4` (Cyan Teal) | `#A7F3D0` (Pale Mint) | `#132E24` | Calming, organic dark theme with subtle ecological undertones. |
| `tokyo-violet` | **Tokyo Dusk** | `#A855F7` (Violet) | `#EC4899` (Rose) | `#818CF8` (Periwinkle) | `#2A1B3D` | Atmospheric developer look with subtle violet highlights. |
| `solaris-copper` | **Solaris Copper** | `#FB923C` (Warm Copper)| `#F43F5E` (Crimson) | `#FDE047` (Warm Gold) | `#331E17` | High-density hardware telemetry feel with warm burnished copper tones. |

---

## 3. Input & Output Container UX Design Architecture

### 3.1 Input Containers & Interaction Patterns

| Container Type | Structure & Visual Styling | Focus / Active State | Best UX Rules |
|---|---|---|---|
| **Command / Search Bar** | Single-line field with 1px `#222531` border, surface `#121319`, leading line search icon, trailing clear `✕` button. | Border shifts smoothly to Primary Accent (e.g. `#38BDF8`). No glowing shadow. | Instant filter execution on keystroke. Pressing clear immediately resets list without dismissing keyboard. |
| **Segmented Pill Pickers** (Intervals, Modes) | Recessed horizontal container (`#121319`), containing discrete pill items (`1s`, `3s`, `5s`, `10s`). | Active pill has `#202330` background, 1px accent outline, bold `#FFFFFF` text. | Single-tap instant switch. Instant haptic feedback. Touch target min 40dp height. |
| **Terminal & Script Input Box** | Monospace text field with matte `#0C0D12` background, integrated action run/send button on the right edge. | Border transitions to accent. Caret blinks at 1s interval. | Preserves history stack (Up/Down arrow or history chip bar). Auto-clears or preserves based on tool context. |
| **Form Fields & Settings Inputs** | Outlined container with top-aligned small label (`11sp`, `#9CA3AF`), input text (`14sp`, `#F4F4F6`), subtle hint. | Hairline accent border. Error state highlights in muted red (`#EF4444`). | Clear error message below field in `#EF4444`. Numbers open numeric keypad automatically. |
| **Pill Switches & Toggles** | Minimalist 20dp high pill track with 16dp circular thumb. Inactive: `#222531` track, `#64748B` thumb. | Active: Accent track (`#38BDF8`), pure white `#FFFFFF` thumb. | Instant smooth slide animation (150ms spring), accompanied by gentle haptic tick. |

### 3.2 Output Containers & Data Telemetry Patterns

| Container Type | Structure & Visual Styling | Interactive Features | Best UX Rules |
|---|---|---|---|
| **Telemetry & Metric Tile** | Compact card (`#121319`), small uppercase label (`10sp`, `#9CA3AF`), large primary metric (`22sp`, bold `#F4F4F6`), secondary unit. | Tap-to-expand or tap-to-copy value. | Numbers update smoothly with no jitter. Monospace formatting used for live counters. |
| **Data Inventory Tables** (IP, MAC, Devices) | Clean hairline row dividers (`#181B24`). Left: Device Name & Type. Right: IP, MAC, Interface in monospace. | Tap any row to copy IP or inspect device details in a popover sheet. | Copy action displays instant toast / banner ("Copied 192.168.1.1 to clipboard"). |
| **Telemetry Charts & Graphs** | Deep matte canvas `#0C0D12`, subtle `#181B24` grid axes, 1.5dp single accent curve, 10% gradient fill under curve. | Touch scrubbing with vertical hairline cursor showing exact timestamp + value. | Zero noisy drop shadows. Time window picker (`1m`, `5m`, `15m`) placed directly above chart. |
| **Console / Log Stream Viewer** | Dedicated matte dark container (`#08090C`), monospace text with 1.45 line-height, timestamp in `#64748B`. | Auto-scroll lock toggle (`Follow / Paused`), search bar, and floating "Copy All" action button. | Color-coded log level tags: `[DEBUG]` `#64748B`, `[INFO]` `#38BDF8`, `[WARN]` `#F59E0B`, `[ERROR]` `#EF4444`. |
| **Collapsible Detail Cards** | Card with header row (title + summary stat + animated chevron). | Tapping header expands/collapses drawer smoothly. | **Must default to collapsed (`false`)** to prevent visual overload on initial load. |
| **Status Callouts & Alerts** | 1px border with 8% surface tint matching semantic status (Emerald for active, Amber for degraded, Red for error). | Includes leading status dot (8dp) and action button if recoverable. | Compact height; dismissible if ephemeral. |

---

## 4. LLM-Generated Dynamic Tools Specification (HTML / CSS / JS)

All LLM-built tools generated in Dynamic Tools Studio must inject and utilize the standard CSS template below so that web-based tools dynamically inherit the user's selected accent theme and look completely identical to native host Android screens:

```css
:root {
  --bg-root: #090a0e;
  --surface-base: #121319;
  --surface-elevated: #181a22;
  --surface-active: #202330;
  --border-default: #222531;
  --border-subtle: #181b24;
  --text-primary: #f4f4f6;
  --text-secondary: #9ca3af;
  --text-muted: #64748b;
  
  /* Dynamic Accent Tokens injected from AppSettingsManager */
  --accent-primary: #38bdf8;
  --accent-secondary: #818cf8;
  --accent-tertiary: #34d399;
  --border-tint: #1e293b;
  
  /* Semantic Status */
  --status-success: #10b981;
  --status-warning: #f59e0b;
  --status-error: #ef4444;
  
  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  --font-mono: ui-monospace, SFMono-Regular, "JetBrains Mono", Menlo, Consolas, monospace;
}

body {
  margin: 0;
  padding: 16px;
  background-color: var(--bg-root);
  color: var(--text-primary);
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
}

/* Card Container */
.card {
  background: var(--surface-base);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
}

/* Primary Outlined Button */
.button {
  background: var(--surface-elevated);
  border: 1px solid var(--border-default);
  color: var(--text-primary);
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.button:active {
  background: var(--surface-active);
  border-color: var(--accent-primary);
}

/* Segmented Control */
.segmented-group {
  display: flex;
  background: var(--surface-base);
  border: 1px solid var(--border-default);
  border-radius: 8px;
  padding: 3px;
}

.segmented-btn {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  padding: 6px;
  font-size: 12px;
  border-radius: 6px;
  cursor: pointer;
}

.segmented-btn.active {
  background: var(--surface-active);
  border: 1px solid var(--accent-primary);
  color: var(--text-primary);
  font-weight: 600;
}

/* Monospace Data Table */
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-family: var(--font-mono);
  font-size: 12px;
}

.data-table td {
  padding: 8px 6px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-secondary);
}

.data-table td.primary {
  color: var(--text-primary);
}
```

---

## 5. Implementation Roadmap for Complete System Migration

1. **`AppSettingsManager.kt` & Accent Palettes**:
   - Add `KEY_ACCENT_PALETTE = "app_accent_palette"` with the 5 curated palettes (`LINEAR_CYAN`, `NOTHING_AMBER`, `NORDIC_EMERALD`, `TOKYO_VIOLET`, `SOLARIS_COPPER`).
   - Add `AccentPalette` model with primary, secondary, tertiary, and border tint colors.
2. **Settings Tab (`SettingsScreen.kt`)**:
   - Add "Accent Color Theme" picker section displaying the 5 palette options with live preview swatches.
3. **Core Colors & Theme (`Color.kt`, `Theme.kt`, `IsometricCards.kt`)**:
   - Replace rainbow glass gradient brushes and saturated neon glows with clean matte obsidian surfaces and 1px hairline borders.
   - Inject active `AccentPalette` dynamically into theme locals or composition locals.
4. **Dashboard (`DashboardScreen.kt`)**:
   - Apply deep obsidian background (`#090A0E`), clean matte cards (`#121319`), hairline borders (`#222531`), and subtle desaturated tool icons.
5. **Native Tools**:
   - Update Terminal, Network Topology, System Info, Sensors, FTP, NFC, LLM Gateway, Dynamic Tools Studio, and System Manual to strictly adhere to the input/output container standards.
6. **Dynamic Tools Studio & Runner**:
   - Update web boilerplate generation to automatically inject the user's active accent palette tokens into CSS variables.
