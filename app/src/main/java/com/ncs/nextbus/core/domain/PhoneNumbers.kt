package com.ncs.nextbus.core.domain

/** Validation for emergency contact numbers. */
object PhoneNumbers {
    private const val MIN_DIGITS = 10
    private const val MAX_DIGITS = 15

    /**
     * Returns the number with formatting removed (spaces, dashes, dots, brackets), keeping a leading `+`,
     * or null if it isn't a plausible phone number.
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        val plus = trimmed.startsWith("+")
        val body = (if (plus) trimmed.drop(1) else trimmed).filterNot { it in " -.()" }
        if (body.isEmpty() || !body.all { it.isDigit() } || body.length !in MIN_DIGITS..MAX_DIGITS) return null
        return if (plus) "+$body" else body
    }
}
