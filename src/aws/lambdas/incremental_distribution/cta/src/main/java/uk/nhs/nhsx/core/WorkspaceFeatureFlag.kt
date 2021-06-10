package uk.nhs.nhsx.core

data class WorkspaceFeatureFlag(val workspace: String, val enabledWorkspaces: List<String>) : FeatureFlag {
    init {
        require(workspace.isNotBlank()) { "workspace must not be blank" }
    }

    override fun isEnabled() =
        (enabledWorkspaces.contains(ALL_SPECIFIER)
            || enabledWorkspaces.contains(BRANCH_SPECIFIER) && !workspace.startsWith(NAMED_ENV_PREFIX)
            || enabledWorkspaces.contains(workspace))

    companion object {
        private const val ALL_SPECIFIER = "*"
        private const val BRANCH_SPECIFIER = "branch"
        private const val NAMED_ENV_PREFIX = "te-"
    }
}
