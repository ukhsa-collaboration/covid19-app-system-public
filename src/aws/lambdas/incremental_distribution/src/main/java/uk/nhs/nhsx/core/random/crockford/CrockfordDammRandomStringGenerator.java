/*
 *
 *  *  Copyright (c) 2020 NHSX
 *
 */
package uk.nhs.nhsx.core.random.crockford;

import com.google.common.io.BaseEncoding;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class CrockfordDammRandomStringGenerator {

    private static final String RAW_BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String CROCKFORD_BASE32_ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz";

    private final DammChecksum checksum = checksum();
    private final BaseEncoding BASE_ENCODER = BaseEncoding.base32();
    private final SecureRandom random;
    private final List<Pattern> disallowedRegexes;

    public CrockfordDammRandomStringGenerator() {
        this(readBannedWordsFromClasspath());
    }

    public CrockfordDammRandomStringGenerator(List<String> bannedWordsRegexList) {
        this(new SecureRandom(), bannedWordsRegexList);
    }

    public CrockfordDammRandomStringGenerator(SecureRandom random, List<String> bannedWordsRegexList) {
        this.random = random;
        this.disallowedRegexes = bannedWordsRegexList.stream()
            .map(Pattern::compile)
            .collect(toList());
    }

    public String generate() {
        while (true) {
            byte[] bytes = new byte[5]; // ceil((7 * 5) / 8)
            random.nextBytes(bytes);
            String rawBase32 = BASE_ENCODER.encode(bytes).substring(0, 7);
            String friendlyCode = convertCrockford(rawBase32);

            String linkingId = friendlyCode + checksum.checksum(friendlyCode);
            if (!isBannedWord(linkingId)) {
                return linkingId;
            }
        }
    }

    private static List<String> readBannedWordsFromClasspath() {
        try {
            URL resource = CrockfordDammRandomStringGenerator.class.getResource("banned-words-regex-list.txt");
            return Files.readAllLines(Paths.get(requireNonNull(resource).toURI()), UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DammChecksum checksum() {
        return new DammChecksum(CROCKFORD_BASE32_ALPHABET);
    }

    public static class DammChecksum {

        private static final int DAMM_MODULUS = 32;
        private static final int DAMM_MASK = 5;

        private final String alphabet;

        public DammChecksum(String alphabet) {
            this.alphabet = alphabet;
        }

        public boolean validate(String string) {
            try {
                if (string == null || string.length() == 0 || string.length() > 10) return false;
                return checksum(string) == '0';
            } catch (StringIndexOutOfBoundsException e) {
                // character given was not in allowed character set
                return false;
            }
        }

        // See https://stackoverflow.com/questions/23431621/extending-the-damm-algorithm-to-base-32
        private char checksum(String linkingId) {
            return alphabet.charAt(linkingId.chars()
                .map(alphabet::indexOf)
                .reduce(0, this::dammOperation));
        }

        private int dammOperation(int checksum, int digit) {
            checksum ^= digit;
            checksum <<= 1;
            if (checksum >= DAMM_MODULUS) {
                checksum = (checksum ^ DAMM_MASK) % DAMM_MODULUS;
            }
            return checksum;
        }
    }

    private String convertCrockford(String rawBase32) {
        StringBuilder builder = new StringBuilder(rawBase32);
        for (int i = 0; i < rawBase32.length(); i++) {
            char raw = rawBase32.charAt(i);
            int value = RAW_BASE32_ALPHABET.indexOf(raw);
            builder.setCharAt(i, CROCKFORD_BASE32_ALPHABET.charAt(value));
        }
        return builder.toString();
    }

    public boolean isBannedWord(String linkingId) {
        String normalisedLinkingId = linkingId
            .replace('4', 'a')
            .replace('8', 'b')
            .replace('3', 'e')
            .replace('9', 'g')
            .replace('5', 's');
        return disallowedRegexes.stream().anyMatch(p -> p.matcher(normalisedLinkingId).find());
    }
}
