package uk.nhs.nhsx.core

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class WorkspaceFeatureFlagTest {

    @Test
    fun `all specifier`() {
        expectThat(WorkspaceFeatureFlag("abc123", listOf("*")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("*")).isEnabled()).isTrue()
    }

    @Test
    fun `branch specifier`() {
        expectThat(WorkspaceFeatureFlag("abc123", listOf("branch")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("branch")).isEnabled()).isFalse()
    }

    @Test
    fun `target env specifier`() {
        expectThat(WorkspaceFeatureFlag("abc123", listOf("abc123")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("abc123")).isEnabled()).isFalse()

        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("te-abc123")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("te-abc123", "*")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("te-abc123", "branch")).isEnabled()).isTrue()
    }

    @Test
    fun `all specifier overrides branch specifier`() {
        expectThat(WorkspaceFeatureFlag("abc123", listOf("*", "branch")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("*", "branch")).isEnabled()).isTrue()
    }

    @Test
    fun `all specifier overrides target env specifier`() {
        expectThat(WorkspaceFeatureFlag("abc123", listOf("*", "branch")).isEnabled()).isTrue()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("abc123", "*")).isEnabled()).isTrue()
    }

    @Test
    fun `empty specifier`() {
        expectThat(WorkspaceFeatureFlag("abc123", emptyList()).isEnabled()).isFalse()
        expectThat(WorkspaceFeatureFlag("te-abc123", listOf("")).isEnabled()).isFalse()
    }

    @Test
    fun `blank args`() {
        expectThrows<IllegalArgumentException> { WorkspaceFeatureFlag("", emptyList()) }
        expectThrows<IllegalArgumentException> { WorkspaceFeatureFlag("   ", emptyList()) }
    }
}
