package uk.nhs.nhsx.core;

@FunctionalInterface
public interface UncheckedException<R> {

    R get() throws Exception;

    static <R> R uncheckedGet(UncheckedException<R> supplier) {
        try { return supplier.get(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}