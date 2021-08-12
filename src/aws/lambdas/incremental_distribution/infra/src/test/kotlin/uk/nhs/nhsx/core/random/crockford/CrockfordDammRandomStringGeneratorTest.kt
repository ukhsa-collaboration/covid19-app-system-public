/*
 * Copyright (c) 2020 NHSX
 */
package uk.nhs.nhsx.core.random.crockford

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isLessThan
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.assertions.matches
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.Companion.checksum
import java.security.SecureRandom
import java.util.*
import java.util.function.Function

class CrockfordDammRandomStringGeneratorTest {

    private val generator = CrockfordDammRandomStringGenerator(bannedWordsRegexList = listOf("u+"))

    @Test
    fun `loads the banned list`() {
        expectThat(CrockfordDammRandomStringGenerator().isBannedWord("0mg")).isTrue()
    }

    @Test
    fun `generate random linking id returns human friendly ids`() {
        expectThat(generator.generate()).matches(Regex("^[$ALPHABET]{8}$"))
    }

    @Test
    fun `generate random linking id returns distinct ids`() {
        val linkingId1 = generator.generate()
        val linkingId2 = generator.generate()
        expectThat(linkingId1).isNotEqualTo(linkingId2)
    }

    @Test
    fun `linking id space should be large enough for many values`() {
        val generator = CrockfordDammRandomStringGenerator(bannedWordsRegexList = listOf())

        val totalNumberOfGeneratedIds = 100000
        val observedIds = mutableSetOf<String>()

        repeat(totalNumberOfGeneratedIds) {
            observedIds.add(generator.generate())
        }

        val duplicates = totalNumberOfGeneratedIds - observedIds.size

        expectThat(duplicates).isLessThan(5)
    }

    @Test
    fun `linking id should contain aValid checksum`() {
        val linkingId = generator.generate()
        expectThat(checksum().validate(linkingId)).isTrue()
    }

    @Test
    fun `linking id should validate words against regex and create another if not allowed`() {
        val cut = CrockfordDammRandomStringGenerator(MockSecureRandom(14578373L), listOf("z3+z", "7[a-z]+"))
        val linkingId = cut.generate()
        expectThat(linkingId).not().matches(Regex(".*z3+z.*"))
        expectThat(linkingId).not().matches(Regex(".*7[a-z]+.*"))
        expectThat(linkingId).isEqualTo("ebz6dxht")
    }

    @Test
    fun `fuzz substitution errors are caught by checksum`() {
        // Repeat this test 20 times
        repeat(20) {
            val baseId = generator.generate()
            expectThat(checksum().validate(baseId)).isTrue()
            for (i in baseId.indices) {
                val modifiedId = replaceCharAt(baseId, i, ::replaceCharRandomly)
                expectThat(checksum().validate(modifiedId)).describedAs(modifiedId).isFalse()
            }
        }
    }

    @Test
    fun `fuzz transposition errors are caught by checksum`() {
        // Repeat this test 20 times
        repeat(20) {
            val baseId = generator.generate()
            expectThat(checksum().validate(baseId)).isTrue()
            for (i in 0 until baseId.length - 1) {
                val modifiedId = swapCharsAt(baseId, i, i + 1)
                if (modifiedId != baseId) {
                    expectThat(checksum().validate(modifiedId)).describedAs(modifiedId).isFalse()
                }
            }
        }
    }

    @Test
    fun `very long strings or strings that dont match pattern are rejected`() {
        expectThat(checksum().validate("000000000000000000000000000000000000000000000000000000000000")).isFalse()
        expectThat(checksum().validate("111111111111111111111111111111111111111111111111111111111111")).isFalse()
    }

    @Test
    fun `is banned word returns true if linking id matches anything in the banned words list`() {
        val cut = CrockfordDammRandomStringGenerator(bannedWordsRegexList = listOf("badges"))
        expectThat(cut.isBannedWord("hello")).isFalse()
        expectThat(cut.isBannedWord("badges")).isTrue()
        expectThat(cut.isBannedWord("84badgesrh")).isTrue()
        expectThat(cut.isBannedWord("84d935")).isTrue()
    }

    private fun replaceCharAt(input: String, position: Int, operation: Function<Char, Char>) =
        StringBuilder(input).apply { setCharAt(position, operation.apply(input[position])) }.toString()

    private fun swapCharsAt(input: String, position1: Int, position2: Int) = StringBuilder(input).apply {
        setCharAt(position1, input[position2])
        setCharAt(position2, input[position1])
    }.toString()

    private fun replaceCharRandomly(c: Char): Char {
        // Picks another hex value randomly
        // never returns the original value
        var random = VAGUELY_RANDOM.nextInt(15)
        if (random >= ALPHABET.indexOf(c)) {
            random++
        }
        return ALPHABET[random]
    }

    /**
     * Random that is more predictable for unit tests without having to sacrifice the type safety of the generator
     */
    private inner class MockSecureRandom(seed: Long) : SecureRandom() {
        val random: Random = Random(seed)
        override fun nextBytes(bytes: ByteArray) {
            random.nextBytes(bytes)
        }
    }

    companion object {
        private const val ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"
        private val VAGUELY_RANDOM = SecureRandom()
    }
}
