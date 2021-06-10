package uk.nhs.nhsx.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class WorkspaceFeatureFlagTest {

    @Test
    fun `all specifier`() {
        assertThat(WorkspaceFeatureFlag("abc123", listOf("*")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("*")).isEnabled()).isTrue
    }

    @Test
    fun `branch specifier`() {
        assertThat(WorkspaceFeatureFlag("abc123", listOf("branch")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("branch")).isEnabled()).isFalse
    }

    @Test
    fun `target env specifier`() {
        assertThat(WorkspaceFeatureFlag("abc123", listOf("abc123")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("abc123")).isEnabled()).isFalse

        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("te-abc123")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("te-abc123", "*")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("te-abc123", "branch")).isEnabled()).isTrue
    }

    @Test
    fun `all specifier overrides branch specifier`() {
        assertThat(WorkspaceFeatureFlag("abc123", listOf("*", "branch")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("*", "branch")).isEnabled()).isTrue
    }

    @Test
    fun `all specifier overrides target env specifier`() {
        assertThat(WorkspaceFeatureFlag("abc123", listOf("*", "branch")).isEnabled()).isTrue
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("abc123", "*")).isEnabled()).isTrue
    }

    @Test
    fun `empty specifier`() {
        assertThat(WorkspaceFeatureFlag("abc123", emptyList()).isEnabled()).isFalse
        assertThat(WorkspaceFeatureFlag("te-abc123", listOf("")).isEnabled()).isFalse
    }

    @Test
    fun `blank args`() {
        assertThatThrownBy { WorkspaceFeatureFlag("", emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy { WorkspaceFeatureFlag("   ", emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

}
