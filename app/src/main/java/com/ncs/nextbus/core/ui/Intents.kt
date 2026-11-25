package com.ncs.nextbus.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.ncs.nextbus.R

/** Opens the dialer pre-filled with [number]. No CALL_PHONE permission needed; the user confirms. */
fun Context.dial(number: String) = launchSafely(Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(number)}".toUri()))

/** Opens the SMS app addressed to [number] with [body] pre-filled. */
fun Context.composeSms(number: String, body: String) = launchSafely(
    Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(number)}".toUri()).putExtra("sms_body", body),
)

/** System share sheet for plain text. */
fun Context.shareText(text: String, chooserTitle: String) = launchSafely(
    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), chooserTitle),
)

private fun Context.launchSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show()
    }
}
