package com.bypassbd.otp.data

/**
 * Turns an IVAC login SMS into a numeric OTP.
 *
 * The IVAC security SMS spells the code out in words, hyphen-separated, e.g.:
 *
 *   "(IVACBD) For security, type the following sequence when prompted
 *    Four-Two-Six-One-Four-Seven ."
 *
 * [extract] walks the words, mapping each number word to its digit, and returns
 * the longest run of consecutive number words (here "426147"). If the message
 * instead carries a plain numeric code it falls back to the first 4–8 digit run,
 * so both SMS styles work.
 *
 * Pure, side-effect-free, and unit-tested (see OtpParserTest) — the network and
 * Android layers depend on this, never the other way round.
 */
object OtpParser {

    private val WORDS: Map<String, Char> = mapOf(
        "zero" to '0', "oh" to '0', "o" to '0',
        "one" to '1', "two" to '2', "three" to '3', "four" to '4', "five" to '5',
        "six" to '6', "seven" to '7', "eight" to '8', "nine" to '9'
    )

    private val LETTERS = Regex("[A-Za-z]+")
    private val NUMERIC = Regex("\\d{4,8}")

    /** True if the message looks like an IVAC OTP SMS worth forwarding. */
    fun looksLikeIvac(sender: String?, body: String?): Boolean {
        val s = (sender ?: "").uppercase()
        val b = (body ?: "").uppercase()
        if (s.contains("IVAC")) return true
        if (b.contains("IVAC")) return true
        if (b.contains("SEQUENCE") && (b.contains("SECURITY") || b.contains("PROMPT"))) return true
        return b.contains("OTP") && extract(body) != null
    }

    /** Extract the 4–8 digit OTP, or null if none is present. */
    fun extract(body: String?): String? {
        if (body.isNullOrBlank()) return null

        // 1) Word sequence — the IVAC style. Longest consecutive run wins.
        var best = ""
        val run = StringBuilder()
        for (m in LETTERS.findAll(body)) {
            val d = WORDS[m.value.lowercase()]
            if (d != null) {
                run.append(d)
            } else {
                if (run.length > best.length) best = run.toString()
                run.setLength(0)
            }
        }
        if (run.length > best.length) best = run.toString()
        if (best.length in 4..8) return best

        // 2) Plain numeric fallback.
        return NUMERIC.find(body)?.value
    }
}
