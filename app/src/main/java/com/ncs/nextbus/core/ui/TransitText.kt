package com.ncs.nextbus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ncs.nextbus.R
import com.ncs.nextbus.core.model.BusActivity
import com.ncs.nextbus.core.model.LiveBus

/** One-line description of what a bus is doing right now. */
@Composable
fun busStatusLine(bus: LiveBus): String = when (bus.activity) {
    BusActivity.LAYOVER -> stringResource(R.string.bus_status_layover, bus.nextStop.stop.name)
    BusActivity.AT_STOP -> stringResource(R.string.bus_status_at_stop, bus.nextStop.stop.name)
    BusActivity.MOVING -> stringResource(R.string.bus_status_next_stop, bus.nextStop.stop.name)
}

/** Seconds until the bus reaches (or, at a terminus, leaves) its next stop. */
fun LiveBus.etaToNextStopSeconds(): Int? = etaSecondsTo(nextStop.stop.id, direction)
