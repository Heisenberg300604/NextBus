package com.ncs.nextbus

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.requestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ncs.nextbus.ui.navigation.NavHostNavigator
import com.ncs.nextbus.ui.theme.NextBusTheme
import com.ncs.nextbus.ui.theme.Charcoal900
import com.ncs.nextbus.ui.theme.Indigo600
import com.ncs.nextbus.ui.theme.OnIndigo50

class MainActivity : ComponentActivity() {

    private val locationPermissionCode = 1
    private var isLocationPermissionGranted by remember { mutableStateOf(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NextBusTheme {
                if (!isLocationPermissionGranted) {
                    PermissionScreen(
                        onGranted = { isLocationPermissionGranted = true },
                        onRequest = { requestLocationPermission() }
                    )
                } else {
                    NavHostNavigator()
                }
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionGranted = true
        } else {
            Toast.makeText(
                this,
                "Location permission required to proceed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Composable
    private fun PermissionScreen(
        onGranted: () -> Unit,
        onRequest: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NextBus",
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Indigo600
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Location permission required",
                fontSize = 16.sp,
                color = Charcoal900
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enable location to see nearby buses and live tracking",
                fontSize = 14.sp,
                color = Charcoal900.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.width(200.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Indigo600
                )
            ) {
                Text(
                    text = "Allow Location Access",
                    color = OnIndigo50
                )
            }
        }
    }
}