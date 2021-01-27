package uk.nhs.nhsx.virology;

import uk.nhs.nhsx.core.ValueType;

public class Country extends ValueType<Country> {

    private Country(String value) {
        super(value);
    }

    public static Country of(String value) {
        return new Country(value);
    }
}
