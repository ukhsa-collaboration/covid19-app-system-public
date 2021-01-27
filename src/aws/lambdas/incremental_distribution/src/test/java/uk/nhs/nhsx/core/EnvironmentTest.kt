package uk.nhs.nhsx.core

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.Environment.EnvironmentName
import uk.nhs.nhsx.core.Environment.EnvironmentType
import uk.nhs.nhsx.core.Environment.fromName
import uk.nhs.nhsx.core.Environment.unknown
import uk.nhs.nhsx.core.TestEnvironments.NOTHING

class EnvironmentTest {

    @Test
    fun nonProduction() {
        assertEnvironmentDeduced(EnvironmentType.NonProduction, "b123fd", "te-ci", "te-test", "te-qa", "te-load-test", "te-staging", "te-demo")
    }

    @Test
    fun production() {
        assertEnvironmentDeduced(EnvironmentType.Production, "te-prod", "te-prodxd", "te-prod1")
    }

    @Test
    fun whenNonProduction() {
        assertThat(fromName("te-ci", NOTHING).whenNonProduction("something") { "ACTIVATED" }.orElse("NOT"), equalTo("ACTIVATED"))
    }

    @Test
    fun whenProduction() {
        assertThat(fromName("te-prod", NOTHING).whenNonProduction("something") { "ACTIVATED" }.orElse("NOT"), equalTo("NOT"))
    }

    @Test
    fun whenUnknown() {
        assertThat(unknown().whenNonProduction("something") { "ACTIVATED" }.orElse("NOT"), equalTo("NOT"))
    }

    private fun assertEnvironmentDeduced(expected: EnvironmentType, vararg workspaces: String) {
        for (workspace in workspaces) {
            val environment = fromName(workspace, NOTHING)
            assertThat(workspace, environment.name, equalTo(EnvironmentName.of(workspace)))
            assertThat(workspace, environment.type, equalTo(expected))
        }
    }

    @Test
    fun stringEnvironmentKey() {
        val key = EnvironmentKey.string("STRING")
        assertThat(TestEnvironments.TEST.apply(mapOf("STRING" to "a nice string")).access.required(key), equalTo("a nice string"))
    }

    @Test
    fun valueEnvironmentKey() {
        val key = EnvironmentKey.value("VALUE", MyValueType::of)
        assertThat(TestEnvironments.TEST.apply(mapOf("VALUE" to "a nice string")).access.required(key), equalTo(MyValueType.of("a nice string")))
    }

    @Test
    fun stringsEnvironmentKey() {
        val key = EnvironmentKey.strings("STRINGS")
        assertThat(TestEnvironments.TEST.apply(mapOf("STRINGS" to "a,b,c")).access.required(key), equalTo(listOf("a", "b", "c")))
        assertThat(TestEnvironments.TEST.apply(mapOf("STRINGS" to "a,,c")).access.required(key), equalTo(listOf("a", "c")))
    }

    @Test
    fun integerEnvironmentKey() {
        val key = EnvironmentKey.integer("INTEGER")
        assertThat(TestEnvironments.TEST.apply(mapOf("INTEGER" to "1")).access.required(key), equalTo(1))
        assertThat(TestEnvironments.TEST.apply(mapOf("INTEGER" to "1234")).access.required(key), equalTo(1234))
    }

    @Test
    fun boolEnvironmentKey() {
        val key = EnvironmentKey.bool("BOOL")
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "true")).access.required(key), equalTo(true))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "True")).access.required(key), equalTo(true))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "false")).access.required(key), equalTo(false))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "False")).access.required(key), equalTo(false))
    }

    @Test
    fun defaultedKey() {
        val key = EnvironmentKey.bool("BOOL")
        assertThat(TestEnvironments.TEST.apply(mapOf()).access.defaulted(key) { false }, equalTo(false))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "true")).access.defaulted(key) { false }, equalTo(true))
    }
}

private class MyValueType private constructor(value: String?) : ValueType<MyValueType?>(value) {
    companion object {
        fun of(value: String?): MyValueType  = MyValueType(value)
    }
}
