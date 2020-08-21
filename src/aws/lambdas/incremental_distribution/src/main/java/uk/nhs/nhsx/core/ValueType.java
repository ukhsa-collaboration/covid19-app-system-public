package uk.nhs.nhsx.core;

public abstract class ValueType<T extends ValueType<T>> implements Comparable<T> {
    public final String value;

    protected ValueType(String value) {
        this.value = value;

        if (value == null) {
            throw new IllegalArgumentException("Cannot have a null " + getClass().getSimpleName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @SuppressWarnings("rawtypes")
		ValueType that = (ValueType) o;

        return value.equals(that.value);

    }

    @Override
    public int hashCode() {
        return value.hashCode();
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
