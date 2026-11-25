package com.ncs.nextbus.core.ui

import com.google.android.gms.maps.model.LatLng
import com.ncs.nextbus.core.model.GeoPoint

fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

/** Shareable map link that opens in any browser or maps app. */
fun GeoPoint.mapsUrl(): String = "https://maps.google.com/?q=%.5f,%.5f".format(java.util.Locale.US, latitude, longitude)
