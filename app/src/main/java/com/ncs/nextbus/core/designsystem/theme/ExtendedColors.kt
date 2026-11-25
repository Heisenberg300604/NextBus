package com.ncs.nextbus.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colours Material 3 has no role for: service status and fuel types. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color,
    val offline: Color,
    val electric: Color,
    val cng: Color,
    val diesel: Color,
    val sos: Color,
    val onSos: Color,
)

internal val LightExtendedColors = ExtendedColors(
    success = Color(0xFF1B7F3B),
    onSuccessContainer = Color(0xFF00210B),
    successContainer = Color(0xFFA3F5B4),
    warning = Color(0xFF8A5300),
    onWarningContainer = Color(0xFF2C1700),
    warningContainer = Color(0xFFFFDDB7),
    offline = Color(0xFF75777F),
    electric = Color(0xFF1B7F3B),
    cng = Color(0xFF00696B),
    diesel = Color(0xFF8A5300),
    sos = Color(0xFFC62828),
    onSos = Color(0xFFFFFFFF),
)

internal val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF88D99A),
    onSuccessContainer = Color(0xFFA3F5B4),
    successContainer = Color(0xFF005321),
    warning = Color(0xFFFFB95C),
    onWarningContainer = Color(0xFFFFDDB7),
    warningContainer = Color(0xFF693F00),
    offline = Color(0xFF8F9099),
    electric = Color(0xFF88D99A),
    cng = Color(0xFF6FD6D8),
    diesel = Color(0xFFFFB95C),
    sos = Color(0xFFFF8A80),
    onSos = Color(0xFF3B0000),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
