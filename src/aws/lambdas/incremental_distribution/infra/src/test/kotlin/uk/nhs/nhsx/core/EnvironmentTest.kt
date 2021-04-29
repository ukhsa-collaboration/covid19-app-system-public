package uk.nhs.nhsx.core

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Environment.Companion.fromName
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.TestEnvironments.NOTHING
import java.time.Duration

class EnvironmentTest {

    @Test
    fun `non production`() {
        assertEnvironmentDeduced(
            EnvironmentType.NonProduction,
            "b123fd",
            "te-ci",
            "te-test",
            "te-qa",
            "te-load-test",
            "te-staging",
            "te-demo"
        )
    }

    @Test
    fun production() {
        assertEnvironmentDeduced(EnvironmentType.Production, "te-prod", "te-prodxd", "te-prod1")
    }

    private fun assertEnvironmentDeduced(expected: EnvironmentType, vararg workspaces: String) {
        for (workspace in workspaces) {
            val environment = fromName(workspace, NOTHING)
            assertThat(workspace, environment.name, equalTo(EnvironmentName.of(workspace)))
            assertThat(workspace, environment.type, equalTo(expected))
        }
    }

    @Test
    fun `string environment key`() {
        val key = EnvironmentKey.string("STRING")
        assertThat(
            TestEnvironments.TEST.apply(mapOf("STRING" to "a nice string")).access.required(key),
            equalTo("a nice string")
        )
    }

    @Test
    fun `value environment key`() {
        val key = EnvironmentKey.value("VALUE", MyValueType)
        assertThat(
            TestEnvironments.TEST.apply(mapOf("VALUE" to "a nice string")).access.required(key),
            equalTo(MyValueType("a nice string"))
        )
    }

    @Test
    fun `strings environment key`() {
        val key = EnvironmentKey.strings("STRINGS")
        assertThat(
            TestEnvironments.TEST.apply(mapOf("STRINGS" to "a,b,c")).access.required(key),
            equalTo(listOf("a", "b", "c"))
        )
        assertThat(
            TestEnvironments.TEST.apply(mapOf("STRINGS" to "a,,c")).access.required(key),
            equalTo(listOf("a", "c"))
        )
    }

    @Test
    fun `integer environment key`() {
        val key = EnvironmentKey.integer("INTEGER")
        assertThat(TestEnvironments.TEST.apply(mapOf("INTEGER" to "1")).access.required(key), equalTo(1))
        assertThat(TestEnvironments.TEST.apply(mapOf("INTEGER" to "1234")).access.required(key), equalTo(1234))
    }

    @Test
    fun `duration environment key`() {
        val key = EnvironmentKey.duration("DURATION")
        assertThat(TestEnvironments.TEST.apply(mapOf("DURATION" to "PT-15M")).access.required(key), equalTo(Duration.ofMinutes(-15)))
        assertThat(TestEnvironments.TEST.apply(mapOf("DURATION" to "PT24H")).access.required(key), equalTo(Duration.ofDays(1)))
    }

    @Test
    fun `bool environment key`() {
        val key = EnvironmentKey.bool("BOOL")
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "true")).access.required(key), equalTo(true))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "True")).access.required(key), equalTo(true))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "false")).access.required(key), equalTo(false))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "False")).access.required(key), equalTo(false))
    }

    @Test
    fun `defaulted key`() {
        val key = EnvironmentKey.bool("BOOL")
        assertThat(TestEnvironments.TEST.apply(mapOf()).access.defaulted(key) { false }, equalTo(false))
        assertThat(TestEnvironments.TEST.apply(mapOf("BOOL" to "true")).access.defaulted(key) { false }, equalTo(true))
    }
}

class MyValueType (value: String) : StringValue(value) {
    companion object : StringValueFactory<MyValueType>(::MyValueType)
}
