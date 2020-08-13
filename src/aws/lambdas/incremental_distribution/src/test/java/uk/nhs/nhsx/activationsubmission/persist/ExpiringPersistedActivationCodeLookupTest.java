package uk.nhs.nhsx.activationsubmission.persist;

import org.junit.Test;
import uk.nhs.nhsx.activationsubmission.ActivationCode;
import uk.nhs.nhsx.activationsubmission.reporting.PersistedActivationCodeReporting;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExpiringPersistedActivationCodeLookupTest {

    private final Duration validity = Duration.ofMinutes(1);
    private final Instant now = Instant.ofEpochSecond(1_000_000);
    private final Supplier<Instant> clock = () -> now;
    private final TestReporting reporting = new TestReporting();

    public static class TestReporting implements PersistedActivationCodeReporting {
        private int missing;
        private int accepted;
        private int expired;

        @Override
        public void missing() {
            missing++;
        }

        @Override
        public void accepted(PersistedActivationCode code) {
            accepted++;
        }

        @Override
        public void expired(PersistedActivationCode code) {
            expired++;
        }
    }

    private PersistedActivationCode aCode(Instant expiry) {
        return new PersistedActivationCode(
            now.minus(Duration.ofMinutes(10)),
            ActivationCodeBatchName.of("some-batch"), ActivationCode.of("123"),
            expiry,
            false
        );
    }

    private PersistedActivationCode anAlreadyUsedCode(Instant expiry) {
        return new PersistedActivationCode(
            now.minus(Duration.ofMinutes(10)),
            ActivationCodeBatchName.of("some-batch"), ActivationCode.of("123"),
            expiry,
            true
        );
    }

    @Test
    public void codeThatExistsAndActivatedRecentlyIsValid() throws Exception {
        PersistedActivationCodeLookup lookup = new ExpiringPersistedActivationCodeLookup(clock, validity, code -> Optional.of(aCode(now)), reporting);
        assertThat(lookup.find(ActivationCode.of("1234")), isPresent());
        assertThat(reporting.accepted, is(1));
        assertThat(reporting.expired, is(0));
        assertThat(reporting.missing, is(0));
    }

    @Test
    public void codeThatDoesNotExistIsInvalid() throws Exception {
        PersistedActivationCodeLookup lookup = new ExpiringPersistedActivationCodeLookup(clock, validity, code -> Optional.empty(), reporting);
        assertThat(lookup.find(ActivationCode.of("1234")), isEmpty());
        assertThat(reporting.accepted, is(0));
        assertThat(reporting.expired, is(0));
        assertThat(reporting.missing, is(1));
    }

    @Test
    public void codeThatExistsAndActivatedWithinValidityPeriodIsValid() throws Exception {
        PersistedActivationCodeLookup lookup = new ExpiringPersistedActivationCodeLookup(clock, validity, code -> Optional.of(aCode(now.minus(validity.dividedBy(2)))), reporting);
        assertThat(lookup.find(ActivationCode.of("1234")), isPresent());
        assertThat(reporting.accepted, is(1));
        assertThat(reporting.expired, is(0));
        assertThat(reporting.missing, is(0));
    }

    @Test
    public void codeThatExistsAndActivatedWithinValidityPeriodIsValidWhenReusedDoesNotContributeToStatistics() throws Exception {
        PersistedActivationCodeLookup lookup = new ExpiringPersistedActivationCodeLookup(clock, validity, code -> Optional.of(anAlreadyUsedCode(now.minus(validity.dividedBy(2)))), reporting);
        assertThat(lookup.find(ActivationCode.of("1234")), isPresent());
        assertThat(reporting.accepted, is(0));
        assertThat(reporting.expired, is(0));
        assertThat(reporting.missing, is(0));
    }

    @Test
    public void codeThatExistsAndActivatedOutsideValidityPeriodIsInvalid() throws Exception {
        PersistedActivationCodeLookup lookup = new ExpiringPersistedActivationCodeLookup(clock, validity, code -> Optional.of(aCode(now.minus(validity))), reporting);
        assertThat(lookup.find(ActivationCode.of("1234")), isEmpty());
        assertThat(reporting.accepted, is(0));
        assertThat(reporting.expired, is(1));
        assertThat(reporting.missing, is(0));
    }
}