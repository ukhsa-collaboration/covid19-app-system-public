package uk.nhs.nhsx.core

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.Environment.Companion.fromName
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.TestEnvironments.NOTHING
import uk.nhs.nhsx.core.TestEnvironments.TEST
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

    @Test
    fun `string environment key`() {
        val key = EnvironmentKey.string("STRING")
        expectThat(TEST.apply(mapOf("STRING" to "a nice string")).access.required(key)).isEqualTo("a nice string")
    }

    @Test
    fun `value environment key`() {
        val key = EnvironmentKey.value("VALUE", MyValueType)
        expectThat(TEST.apply(mapOf("VALUE" to "a nice string")).access.required(key)).isEqualTo(MyValueType("a nice string"))
    }

    @Test
    fun `strings environment key`() {
        val key = EnvironmentKey.strings("STRINGS")
        expectThat(TEST.apply(mapOf("STRINGS" to "a,b,c")).access.required(key)).isEqualTo(listOf("a", "b", "c"))
        expectThat(TEST.apply(mapOf("STRINGS" to "a,,c")).access.required(key)).isEqualTo(listOf("a", "c"))
    }

    @Test
    fun `integer environment key`() {
        val key = EnvironmentKey.integer("INTEGER")
        expectThat(TEST.apply(mapOf("INTEGER" to "1")).access.required(key)).isEqualTo(1)
        expectThat(TEST.apply(mapOf("INTEGER" to "1234")).access.required(key)).isEqualTo(1234)
    }

    @Test
    fun `duration environment key`() {
        val key = EnvironmentKey.duration("DURATION")
        expectThat(TEST.apply(mapOf("DURATION" to "PT-15M")).access.required(key)).isEqualTo(Duration.ofMinutes(-15))
        expectThat(TEST.apply(mapOf("DURATION" to "PT24H")).access.required(key)).isEqualTo(Duration.ofDays(1))
    }

    @Test
    fun `bool environment key`() {
        val key = EnvironmentKey.bool("BOOL")
        expectThat(TEST.apply(mapOf("BOOL" to "true")).access.required(key)).isTrue()
        expectThat(TEST.apply(mapOf("BOOL" to "True")).access.required(key)).isTrue()
        expectThat(TEST.apply(mapOf("BOOL" to "false")).access.required(key)).isFalse()
        expectThat(TEST.apply(mapOf("BOOL" to "False")).access.required(key)).isFalse()
    }

    @Test
    fun `defaulted key`() {
        val key = EnvironmentKey.bool("BOOL")
        expectThat(TEST.apply(mapOf()).access.defaulted(key) { false }).isFalse()
        expectThat(TEST.apply(mapOf("BOOL" to "true")).access.defaulted(key) { false }).isTrue()
    }

    private fun assertEnvironmentDeduced(expected: EnvironmentType, vararg workspaces: String) {
        for (workspace in workspaces) {
            val environment = fromName(workspace, NOTHING)
            expectThat(environment).describedAs(workspace).and {
                get(Environment::name).isEqualTo(EnvironmentName.of(workspace))
                get(Environment::type).isEqualTo(expected)
            }
        }
    }
}

class MyValueType(value: String) : StringValue(value) {
    companion object : StringValueFactory<MyValueType>(::MyValueType)
}
