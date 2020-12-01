/*
 *
 *  *  Copyright (c) 2020 NHSX
 *
 */
package uk.nhs.nhsx.core.random.crockford;


import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.checksum;

public class CrockfordDammRandomStringGeneratorTest {
    private static final String ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz";

    private final CrockfordDammRandomStringGenerator generator = new CrockfordDammRandomStringGenerator(Stream.of("u+"));

    @Test
    public void loadsTheBannedList() {
        assertThat(new CrockfordDammRandomStringGenerator().isBannedWord("0mg")).isTrue();
    }

    @Test
    public void generateRandomLinkingIdReturnsHumanFriendlyIds() {
        String linkingId = generator.generate();

        assertThat(linkingId).matches("^[" + ALPHABET + "]{8}$");
    }

    @Test
    public void generateRandomLinkingIdReturnsDistinctIds() {
        String linkingId1 = generator.generate();
        String linkingId2 = generator.generate();

        assertThat(linkingId1).isNotEqualTo(linkingId2);
    }

    @Test
    public void linkingIdSpaceShouldBeLargeEnoughForManyValues() {
        CrockfordDammRandomStringGenerator fasterService = new CrockfordDammRandomStringGenerator(Stream.empty());

        int totalIds = 100_000;

        Set<String> observedIds = new HashSet<>();
        for (int i = 0; i < totalIds; i++) {
            observedIds.add(fasterService.generate());
        }
        int duplicates = totalIds - observedIds.size();

        assertThat(duplicates).isLessThan(5);
    }

    @Test
    public void linkingIdShouldContainAValidChecksum() {
        String linkingId = generator.generate();

        assertThat(checksum().validate(linkingId)).isTrue();
    }

    @Test
    public void linkingIdShouldValidateWordsAgainstRegexAndCreateAnotherIfNotAllowed() {
        CrockfordDammRandomStringGenerator cut = new CrockfordDammRandomStringGenerator(new MockSecureRandom(14578373L), Stream.of("z3+z", "7[a-z]+"));
        String linkingId = cut.generate();

        assertThat(linkingId).doesNotMatch(".*z3+z.*");
        assertThat(linkingId).doesNotMatch(".*7[a-z]+.*");
        assertThat(linkingId).isEqualTo("ebz6dxht");
    }

    @Test
    public void fuzz_substitutionErrorsAreCaughtByChecksum() {
        // Repeat this test 20 times
        IntStream.range(0, 20).forEach(it -> {
            String baseId = generator.generate();
            assertThat(checksum().validate(baseId)).isTrue();

            for (int i = 0; i < baseId.length(); i++) {
                String modifiedId = replaceCharAt(baseId, i, this::replaceCharRandomly);
                assertThat(checksum().validate(modifiedId)).as(modifiedId).isFalse();
            }
        });
    }

    @Test
    public void fuzz_transpositionErrorsAreCaughtByChecksum() {
        // Repeat this test 20 times
        IntStream.range(0, 20).forEach(it -> {
            String baseId = generator.generate();
            assertThat(checksum().validate(baseId)).isTrue();

            for (int i = 0; i < baseId.length() - 1; i++) {
                String modifiedId = swapCharsAt(baseId, i, i + 1);
                if (!modifiedId.equals(baseId)) {
                    assertThat(checksum().validate(modifiedId)).as(modifiedId).isFalse();
                }
            }
        });
    }

    @Test
    public void veryLongStringsOrStringsThatDontMatchPatternAreRejected() {
        assertThat(checksum().validate("000000000000000000000000000000000000000000000000000000000000")).isFalse();
        assertThat(checksum().validate("111111111111111111111111111111111111111111111111111111111111")).isFalse();
    }

    @Test
    public void isBannedWordReturnsTrueIfLinkingIdMatchesAnythingInTheBannedWordsList() {
        CrockfordDammRandomStringGenerator cut = new CrockfordDammRandomStringGenerator(null, Stream.of("badges"));
        assertThat(cut.isBannedWord("hello")).isFalse();
        assertThat(cut.isBannedWord("badges")).isTrue();
        assertThat(cut.isBannedWord("84badgesrh")).isTrue();
        assertThat(cut.isBannedWord("84d935")).isTrue();
    }

    private String replaceCharAt(String input, int position, Function<Character, Character> operation) {
        StringBuilder builder = new StringBuilder(input);
        builder.setCharAt(position, operation.apply(input.charAt(position)));
        return builder.toString();
    }

    private String swapCharsAt(String input, int position1, int position2) {
        StringBuilder builder = new StringBuilder(input);
        builder.setCharAt(position1, input.charAt(position2));
        builder.setCharAt(position2, input.charAt(position1));
        return builder.toString();
    }

    private static final SecureRandom VAGUELY_RANDOM = new SecureRandom();

    private char replaceCharRandomly(char c) {
        // Picks another hex value randomly
        // never returns the original value
        int random = VAGUELY_RANDOM.nextInt(15);
        if (random >= ALPHABET.indexOf(c)) {
            random++;
        }
        return ALPHABET.charAt(random);
    }

    /**
     * Random that is more predictable for unit tests without having to sacrifice the type safety of the generator
     */
    private class MockSecureRandom extends SecureRandom {
		private static final long serialVersionUID = 1L;
		
		final Random random;
        MockSecureRandom(long seed) {
            this.random = new Random(seed);
        }

        @Override
        public void nextBytes(byte[] bytes) {
            random.nextBytes(bytes);
        }
    }

}
