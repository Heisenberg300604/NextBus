package com.ncs.nextbus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

// ── Transit Color Palette ──
val Indigo600 = Color(0xFF2563EB)
val Indigo500 = Color(0xFF3B82F6)
val Indigo400 = Color(0xFF60A5FA)
val Indigo900 = Color(0xFF1E3A8A)

val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald600 = Color(0xFF059669)

val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFB923C)

val Charcoal900 = Color(0xFF0F172A)
val Charcoal800 = Color(0xFF1E293B)
val Charcoal700 = Color(0xFF334155)
val Surface100 = Color(0xFFF8FAFC)
val Surface0 = Color(0xFFFFFFFF)
val Surface50 = Color(0xFFF1F5F9)

// ── On-colors ──
val OnIndigo50 = Color(0xFFEFF8FF)
val OnEmerald50 = Color(0xFFECFDF5)
val OnAmber50 = Color(0xFF1E293B)
val OnCharcoal50 = Color(0xFFFFFFFF)
val OnSurface50 = Color(0xFF64748B)

// ── Glassmorphism Utilities ──
fun glassSurface(color: Color = Charcoal900.copy(alpha = 0.08f), border: Color = Color(0xFF000000).copy(alpha = 0.12f)): Brush =
    Brush.verticalGradient(
        0f to color.copy(alpha = 0.1f),
        0.5f to color.copy(alpha = 0.05f),
        1f to color.copy(alpha = 0.08f)
    )

fun glassBorder(color: Color = Color(0xFF000000).copy(alpha = 0.12f)): Brush =
    Brush.verticalGradient(
        0f to color.copy(alpha = 0.15f),
        0.5f to color.copy(alpha = 0.08f),
        1f to color.copy(alpha = 0.15f)
    )

// ── Glow effects ──
val IndigoGlow = Color(0xFF2563EB).copy(alpha = 0.3f)
val EmeraldGlow = Color(0xFF10B981).copy(alpha = 0.3f)
val AmberGlow = Color(0xFFF59E0B).copy(alpha = 0.3f)