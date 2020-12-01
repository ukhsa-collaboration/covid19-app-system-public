package uk.nhs.nhsx.core;

import java.util.List;

public class WorkspaceFeatureFlag implements FeatureFlag {

    private static final String ALL_SPECIFIER = "*";
    private static final String BRANCH_SPECIFIER = "branch";
    private static final String NAMED_ENV_PREFIX = "te-";

    private final String workspace;
    private final List<String> enabledWorkspaces;

    public WorkspaceFeatureFlag(String workspace, List<String> enabledWorkspaces) {
        if (workspace == null || workspace.isBlank()) {
            throw new IllegalArgumentException("workspace must not be blank");
        }

        if (enabledWorkspaces == null) {
            throw new IllegalArgumentException("enabled workspaces must be a non-null list");
        }

        this.workspace = workspace;
        this.enabledWorkspaces = enabledWorkspaces;
    }

    public boolean isEnabled() {
        return enabledWorkspaces.contains(ALL_SPECIFIER)
            || (enabledWorkspaces.contains(BRANCH_SPECIFIER) && !workspace.startsWith(NAMED_ENV_PREFIX))
            || enabledWorkspaces.contains(workspace);
    }

}
