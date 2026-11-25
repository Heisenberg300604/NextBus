package com.ncs.nextbus.core.common

import javax.inject.Inject

/** Wall-clock abstraction so time-dependent logic (simulation, freshness, schedules) is testable. */
fun interface TimeSource {
    fun nowMillis(): Long
}

class SystemTimeSource @Inject constructor() : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
