package uk.nhs.nhsx.virology;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Environment.EnvironmentKey;
import uk.nhs.nhsx.core.FeatureFlag;

import static java.util.Objects.requireNonNull;
import static uk.nhs.nhsx.core.Environment.EnvironmentKey.bool;

public class VirologyV2ApiFeatureFlag implements FeatureFlag {

    public static final EnvironmentKey<Boolean> VIROLOGY_V2_APIS_ENABLED = bool("virology_v2_apis_enabled");

    private final Environment environment;

    public VirologyV2ApiFeatureFlag(Environment environment) {
        this.environment = environment;
    }

    public static VirologyV2ApiFeatureFlag from(Environment environment) {
        return new VirologyV2ApiFeatureFlag(requireNonNull(environment, "environment must be present"));
    }

    @Override
    public boolean isEnabled() {
        return environment.access.defaulted(VIROLOGY_V2_APIS_ENABLED, () -> false);
    }
}
