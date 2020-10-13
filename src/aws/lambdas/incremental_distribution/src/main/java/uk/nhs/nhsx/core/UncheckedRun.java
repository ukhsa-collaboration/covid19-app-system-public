package uk.nhs.nhsx.core;

@FunctionalInterface
public interface UncheckedRun {

    void run() throws Exception;

    static void uncheckedRun(UncheckedRun runnable) {
        try { runnable.run(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}