package uk.nhs.nhsx.core;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class EnvironmentTest {

    @Test
    public void nonProduction() throws Exception {
        assertEnvironmentDeduced(Environment.EnvironmentType.NonProduction, "b123fd", "te-ci", "te-test", "te-qa", "te-load-test", "te-staging", "te-demo");
    }

    @Test
    public void production() throws Exception {
        assertEnvironmentDeduced(Environment.EnvironmentType.Production, "te-prod", "te-prodxd", "te-prod1");
    }

    @Test
    public void whenNonProduction() throws Exception {
        assertThat(Environment.fromName("te-ci", n -> Optional.empty()).whenNonProduction("something", (e) -> "ACTIVATED").orElse("NOT"), equalTo("ACTIVATED"));
    }

    @Test
    public void whenProduction() throws Exception {
        assertThat(Environment.fromName("te-prod", n -> Optional.empty()).whenNonProduction("something", (e) -> "ACTIVATED").orElse("NOT"), equalTo("NOT"));
    }

    @Test
    public void whenUnknown() throws Exception {
        assertThat(Environment.unknown().whenNonProduction("something", (e) -> "ACTIVATED").orElse("NOT"), equalTo("NOT"));
    }

    private void assertEnvironmentDeduced(Environment.EnvironmentType expected, String... workspaces) {
        for (String workspace : workspaces) {
            Environment environment = Environment.fromName(workspace, n -> Optional.empty());
            assertThat(workspace, environment.name, equalTo(Environment.EnvironmentName.of(workspace)));
            assertThat(workspace, environment.type, equalTo(expected));
        }
    }
}