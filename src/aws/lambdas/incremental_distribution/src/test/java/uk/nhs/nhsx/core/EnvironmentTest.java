package uk.nhs.nhsx.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.nhs.nhsx.core.TestEnvironments.NOTHING;

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
        assertThat(Environment.fromName("te-ci", NOTHING).whenNonProduction("something", (e) -> "ACTIVATED").orElse("NOT"), equalTo("ACTIVATED"));
    }

    @Test
    public void whenProduction() throws Exception {
        assertThat(Environment.fromName("te-prod", NOTHING).whenNonProduction("something", (e) -> "ACTIVATED").orElse("NOT"), equalTo("NOT"));
    }

    @Test
    public void whenUnknown() throws Exception {
        assertThat(Environment.unknown().whenNonProduction("something", (e) -> "ACTIVATED").orElse("NOT"), equalTo("NOT"));
    }

    private void assertEnvironmentDeduced(Environment.EnvironmentType expected, String... workspaces) {
        for (String workspace : workspaces) {
            Environment environment = Environment.fromName(workspace, NOTHING);
            assertThat(workspace, environment.name, equalTo(Environment.EnvironmentName.of(workspace)));
            assertThat(workspace, environment.type, equalTo(expected));
        }
    }

    @Test
    public void stringEnvironmentKey() throws Exception {
        Environment.EnvironmentKey<String> key = Environment.EnvironmentKey.string("STRING");
        assertThat(TestEnvironments.TEST.apply(Map.of("STRING", "a nice string")).access.required(key), equalTo("a nice string"));
    }

    @Test
    public void valueEnvironmentKey() throws Exception {
        Environment.EnvironmentKey<MyValueType> key = Environment.EnvironmentKey.value("VALUE", MyValueType.class);
        assertThat(TestEnvironments.TEST.apply(Map.of("VALUE", "a nice string")).access.required(key), equalTo(MyValueType.of("a nice string")));
    }

    @Test
    public void stringsEnvironmentKey() throws Exception {
        Environment.EnvironmentKey<List<String>> key = Environment.EnvironmentKey.strings("STRINGS");
        assertThat(TestEnvironments.TEST.apply(Map.of("STRINGS", "a,b,c")).access.required(key), equalTo(List.of("a", "b", "c")));
        assertThat(TestEnvironments.TEST.apply(Map.of("STRINGS", "a,,c")).access.required(key), equalTo(List.of("a", "c")));
    }

    @Test
    public void integerEnvironmentKey() throws Exception {
        Environment.EnvironmentKey<Integer> key = Environment.EnvironmentKey.integer("INTEGER");
        assertThat(TestEnvironments.TEST.apply(Map.of("INTEGER", "1")).access.required(key), equalTo(1));
        assertThat(TestEnvironments.TEST.apply(Map.of("INTEGER", "1234")).access.required(key), equalTo(1234));
    }

    @Test
    public void boolEnvironmentKey() throws Exception {
        Environment.EnvironmentKey<Boolean> key = Environment.EnvironmentKey.bool("BOOL");
        assertThat(TestEnvironments.TEST.apply(Map.of("BOOL", "true")).access.required(key), equalTo(true));
        assertThat(TestEnvironments.TEST.apply(Map.of("BOOL", "True")).access.required(key), equalTo(true));
        assertThat(TestEnvironments.TEST.apply(Map.of("BOOL", "false")).access.required(key), equalTo(false));
        assertThat(TestEnvironments.TEST.apply(Map.of("BOOL", "False")).access.required(key), equalTo(false));
    }

    public static class MyValueType extends ValueType<MyValueType> {
        protected MyValueType(String value) {
            super(value);
        }

        public static MyValueType of(String value) {
            return new MyValueType(value);
        }
    }
}