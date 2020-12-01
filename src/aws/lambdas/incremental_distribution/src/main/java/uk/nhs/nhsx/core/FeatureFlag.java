package uk.nhs.nhsx.core;

@FunctionalInterface
public interface FeatureFlag {
    boolean isEnabled();
}
