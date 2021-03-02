package uk.nhs.nhsx.core;

import java.util.Objects;

import static java.lang.String.format;
import static uk.nhs.nhsx.core.Preconditions.checkArgument;

public abstract class ValueType<T extends ValueType<T>> implements Comparable<T> {
    public final String value;

    protected ValueType(String value) {
        checkArgument(value != null, format("Cannot have a null %s", getClass().getSimpleName()));
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueType<?> valueType = (ValueType<?>) o;
        return Objects.equals(value, valueType.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public int compareTo(T other) {
        if (this.getClass().equals(other.getClass())) {
            return this.value.compareTo(other.value);
        }
        throw new ClassCastException("Can only compare exactly same types");
    }
}
