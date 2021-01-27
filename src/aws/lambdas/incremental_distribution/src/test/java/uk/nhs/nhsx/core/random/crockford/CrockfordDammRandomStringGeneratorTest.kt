/*
 *
 *  *  Copyright (c) 2020 NHSX
 *
 */
package uk.nhs.nhsx.core.random.crockford

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.checksum
import java.security.SecureRandom
import java.util.*
import java.util.function.Function

class CrockfordDammRandomStringGeneratorTest {

    private val generator = CrockfordDammRandomStringGenerator(listOf("u+"))

    @Test
    fun loadsTheBannedList() {
        assertThat(CrockfordDammRandomStringGenerator().isBannedWord("0mg")).isTrue
    }

    @Test
    fun generateRandomLinkingIdReturnsHumanFriendlyIds() {
        val linkingId = generator.generate()
        assertThat(linkingId).matches("^[$ALPHABET]{8}$")
    }

    @Test
    fun generateRandomLinkingIdReturnsDistinctIds() {
        val linkingId1 = generator.generate()
        val linkingId2 = generator.generate()
        assertThat(linkingId1).isNotEqualTo(linkingId2)
    }

    @Test
    fun linkingIdSpaceShouldBeLargeEnoughForManyValues() {
        val fasterService = CrockfordDammRandomStringGenerator(listOf())
        val totalIds = 100000
        val observedIds: MutableSet<String> = HashSet()
        for (i in 0 until totalIds) {
            observedIds.add(fasterService.generate())
        }
        val duplicates = totalIds - observedIds.size
        assertThat(duplicates).isLessThan(5)
    }

    @Test
    fun linkingIdShouldContainAValidChecksum() {
        val linkingId = generator.generate()
        assertThat(checksum().validate(linkingId)).isTrue
    }

    @Test
    fun linkingIdShouldValidateWordsAgainstRegexAndCreateAnotherIfNotAllowed() {
        val cut = CrockfordDammRandomStringGenerator(MockSecureRandom(14578373L), listOf("z3+z", "7[a-z]+"))
        val linkingId = cut.generate()
        assertThat(linkingId).doesNotMatch(".*z3+z.*")
        assertThat(linkingId).doesNotMatch(".*7[a-z]+.*")
        assertThat(linkingId).isEqualTo("ebz6dxht")
    }

    @Test
    fun fuzz_substitutionErrorsAreCaughtByChecksum() {
        // Repeat this test 20 times
        repeat(20) {
            val baseId = generator.generate()
            assertThat(checksum().validate(baseId)).isTrue
            for (i in baseId.indices) {
                val modifiedId = replaceCharAt(baseId, i) { c -> replaceCharRandomly(c) }
                assertThat(checksum().validate(modifiedId)).`as`(modifiedId).isFalse
            }
        }
    }

    @Test
    fun fuzz_transpositionErrorsAreCaughtByChecksum() {
        // Repeat this test 20 times
        repeat(20) {
            val baseId = generator.generate()
            assertThat(checksum().validate(baseId)).isTrue
            for (i in 0 until baseId.length - 1) {
                val modifiedId = swapCharsAt(baseId, i, i + 1)
                if (modifiedId != baseId) {
                    assertThat(checksum().validate(modifiedId)).`as`(modifiedId).isFalse
                }
            }
        }
    }

    @Test
    fun veryLongStringsOrStringsThatDontMatchPatternAreRejected() {
        assertThat(checksum().validate("000000000000000000000000000000000000000000000000000000000000")).isFalse
        assertThat(checksum().validate("111111111111111111111111111111111111111111111111111111111111")).isFalse
    }

    @Test
    fun isBannedWordReturnsTrueIfLinkingIdMatchesAnythingInTheBannedWordsList() {
        val cut = CrockfordDammRandomStringGenerator(null, listOf("badges"))
        assertThat(cut.isBannedWord("hello")).isFalse
        assertThat(cut.isBannedWord("badges")).isTrue
        assertThat(cut.isBannedWord("84badgesrh")).isTrue
        assertThat(cut.isBannedWord("84d935")).isTrue
    }

    private fun replaceCharAt(input: String, position: Int, operation: Function<Char, Char>): String {
        val builder = StringBuilder(input)
        builder.setCharAt(position, operation.apply(input[position]))
        return builder.toString()
    }

    private fun swapCharsAt(input: String, position1: Int, position2: Int): String {
        val builder = StringBuilder(input)
        builder.setCharAt(position1, input[position2])
        builder.setCharAt(position2, input[position1])
        return builder.toString()
    }

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
