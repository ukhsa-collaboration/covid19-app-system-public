package uk.nhs.nhsx.virology;

import java.util.Objects;
import java.util.Set;

import static uk.nhs.nhsx.virology.TestKit.LAB_RESULT;
import static uk.nhs.nhsx.virology.TestKit.RAPID_RESULT;

public class CountryTestKitWhitelist {

    private static final Set<CountryTestKitPair> supportedCountryTestKitPairs = Set.of(
        CountryTestKitPair.of(Country.of("England"), LAB_RESULT),
        CountryTestKitPair.of(Country.of("England"), RAPID_RESULT),
        CountryTestKitPair.of(Country.of("Wales"), LAB_RESULT),
        CountryTestKitPair.of(Country.of("Wales"), RAPID_RESULT)
    );

    public static boolean isDiagnosisKeysSubmissionSupported(CountryTestKitPair countryTestKitPair) {
        return supportedCountryTestKitPairs.contains(countryTestKitPair);
    }

    public static class CountryTestKitPair {

        private final Country country;
        private final TestKit testKit;

        private CountryTestKitPair(Country country, TestKit testKit) {
            this.country = country;
            this.testKit = testKit;
        }

        public static CountryTestKitPair of(Country country, TestKit testKit) {
            return new CountryTestKitPair(country, testKit);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CountryTestKitPair that = (CountryTestKitPair) o;
            return Objects.equals(country, that.country) && Objects.equals(testKit, that.testKit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(country, testKit);
        }
    }


}
