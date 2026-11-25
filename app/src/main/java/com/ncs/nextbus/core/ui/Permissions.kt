package com.ncs.nextbus.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState

val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * Returns a function that asks for location permission. [onResult] receives whether any location
 * permission (precise or approximate) was granted.
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        callback.value(result.values.any { it })
    }
    return { launcher.launch(LocationPermissions) }
}

/** Opens this app's system settings page (for permissions the user denied permanently). */
fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.hasLocationPermission(): Boolean = LocationPermissions.any {
    androidx.core.content.ContextCompat.checkSelfPermission(this, it) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}
