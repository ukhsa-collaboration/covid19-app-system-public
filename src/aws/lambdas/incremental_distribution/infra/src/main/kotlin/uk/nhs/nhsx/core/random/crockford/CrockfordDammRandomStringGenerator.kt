package uk.nhs.nhsx.core.random.crockford

import org.apache.commons.codec.binary.Base32
import java.security.SecureRandom
import java.util.regex.Pattern

class CrockfordDammRandomStringGenerator(
    private val random: SecureRandom = SecureRandom(),
    bannedWordsRegexList: List<String> = readBannedWordsFromClasspath()
) {
    private val checksum = checksum()
    private val baseEncoder = Base32(0)
    private val disallowedRegexes: List<Pattern> = bannedWordsRegexList.map { Pattern.compile(it) }

    fun generate(): String {
        while (true) {
            val bytes = ByteArray(5) // ceil((7 * 5) / 8)
            random.nextBytes(bytes)
            val rawBase32 = baseEncoder.encodeAsString(bytes).substring(0, 7)
            val friendlyCode = convertCrockford(rawBase32)
            val linkingId = friendlyCode + checksum.checksum(friendlyCode)
            if (!isBannedWord(linkingId)) {
                return linkingId
            }
        }
    }

    private fun convertCrockford(rawBase32: String): String {
        val builder = StringBuilder(rawBase32)
        for (i in rawBase32.indices) {
            val raw = rawBase32[i]
            val value = RAW_BASE32_ALPHABET.indexOf(raw)
            builder.setCharAt(i, CROCKFORD_BASE32_ALPHABET[value])
        }
        return builder.toString()
    }

    fun isBannedWord(linkingId: String): Boolean {
        val normalisedLinkingId = linkingId
            .replace('4', 'a')
            .replace('8', 'b')
            .replace('3', 'e')
            .replace('9', 'g')
            .replace('5', 's')
        return disallowedRegexes.any { it.matcher(normalisedLinkingId).find() }
    }

    companion object {
        private const val RAW_BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        private const val CROCKFORD_BASE32_ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"

        fun checksum(): DammChecksum = DammChecksum(CROCKFORD_BASE32_ALPHABET)

        private fun readBannedWordsFromClasspath(): List<String> =
            CrockfordDammRandomStringGenerator::class.java
                .getResourceAsStream("banned-words-regex-list.txt")!!
                .reader()
                .readLines()
    }
}

class DammChecksum(private val alphabet: String) {
    fun validate(string: String): Boolean = try {
        when {
            string.isEmpty() || string.length > 10 -> false
            else -> checksum(string) == '0'
        }
    } catch (e: StringIndexOutOfBoundsException) {
        // character given was not in allowed character set
        false
    }

    // See https://stackoverflow.com/questions/23431621/extending-the-damm-algorithm-to-base-32
    fun checksum(linkingId: String): Char = alphabet[linkingId.chars()
        .map { alphabet.indexOf(it.toChar()) }
        .reduce(0) { checksum, digit -> dammOperation(checksum, digit) }]

    private fun dammOperation(checksum: Int, digit: Int): Int {
        var result = checksum
        result = result xor digit
        result = result shl 1
        if (result >= DAMM_MODULUS) {
            result = (result xor DAMM_MASK) % DAMM_MODULUS
        }
        return result
    }

    companion object {
        private const val DAMM_MODULUS = 32
        private const val DAMM_MASK = 5
    }
}
