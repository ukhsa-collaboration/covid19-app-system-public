package uk.nhs.nhsx.core.exceptions;

public class ExceptionPrinting {
    public static String describeCauseOf(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage());
        while (hasCause(e)) {
            e = e.getCause();
            sb.append("; because ");
            sb.append(e.getMessage());
        }
        return sb.toString();
    }

    private static boolean hasCause(Throwable e) {
        return e.getCause() != null && e.getCause() != e;
    }
}
