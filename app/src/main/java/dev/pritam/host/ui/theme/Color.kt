package dev.pritam.host.ui.theme

import androidx.compose.ui.graphics.Color

// Accent Palette Default Tokens (Desaturated / Accessible)
val Cyan = Color(0xFF38BDF8)       // Ice Blue
val CyanDim = Color(0xFF0C4A6E)
val Violet = Color(0xFFA855F7)     // Tokyo Violet
val VioletDim = Color(0xFF581C87)
val Rose = Color(0xFFF43F5E)       // Crimson Rose
val RoseDim = Color(0xFF881337)
val Emerald = Color(0xFF10B981)    // Nordic Emerald
val Amber = Color(0xFFF59E0B)      // Industrial Amber

// Plain Minimalist Dark Surface & Background Tokens
val SpaceBackground = Color(0xFF090A0E)      // Deep Matte Obsidian Root
val SpaceBackgroundEnd = Color(0xFF0D0E14)   // Subtle Vignette Base

// Stepped Matte Surfaces (Elevation without drop shadows)
val GlassSurface = Color(0xFF121319)          // Surface Base (Cards, Tiles)
val GlassSurfaceElevated = Color(0xFF181A22)  // Surface Elevated (Modals, Popovers)
val GlassSurfaceDeep = Color(0xFF121319)      // Surface Base
val GlassSurfaceUltra = Color(0xFF0E0F14)     // Recessed containers

// High-contrast, non-glare dialog & sheet surfaces
val GlassDialogSurface = Color(0xF814161F)
val GlassDialogSurfaceSolid = Color(0xFF14161F)

// Hairline 1px Borders & Dividers (replaces neon and rainbow glows)
val GlassBorder = Color(0xFF222531)           // Standard 1px Hairline Border
val GlassBorderHighlight = Color(0xFF2D3142)  // Focus / subtle lift border
val GlassBorderSubtle = Color(0xFF181B24)     // Hairline inner divider

// Legacy compatibility aliases mapped to minimalist surfaces
val SurfaceDeep = GlassSurfaceDeep
val SurfaceElevated = GlassSurfaceElevated
val SurfaceVariantDeep = GlassSurface
val OutlineDeep = Color(0xFF222531)

// Typography Text Tokens
val TextPrimary = Color(0xFFF4F4F6)           // Pure High-Contrast White
val TextSecondary = Color(0xFF9CA3AF)         // Muted Neutral Gray
val TextTertiary = Color(0xFF64748B)          // Deep Slate Gray for metadata