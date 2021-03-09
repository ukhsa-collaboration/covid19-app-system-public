package uk.nhs.nhsx.circuitbreakers.utils

import java.security.SecureRandom

object TokenGenerator {// letter 'z'// numeral '0'
    /**
     * Generates alphanumeric token of 50 characters
     */
    @JvmStatic
    val token: String
        get() {
            val startIndex = 48 // numeral '0'
            val endIndex = 122 // letter 'z'
            val limit = 50
            val random = SecureRandom()
            return random.ints(startIndex, endIndex + 1)
                .filter { i: Int -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97) }
                .limit(limit.toLong())
                .collect(
                    { StringBuilder() },
                    { obj: StringBuilder, codePoint: Int -> obj.appendCodePoint(codePoint) }) { obj: StringBuilder, s: StringBuilder? ->
                    obj.append(
                        s
                    )
                }
                .toString()
        }
}
