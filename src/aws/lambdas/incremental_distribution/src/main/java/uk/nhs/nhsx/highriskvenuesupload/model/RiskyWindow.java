package uk.nhs.nhsx.highriskvenuesupload.model;

import java.util.Objects;

public class RiskyWindow {
    public final String from;
    public String until;

    public RiskyWindow(String from, String until) {
        this.from = from;
        this.until = until;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskyWindow that = (RiskyWindow) o;
        return from.equals(that.from) &&
                until.equals(that.until);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, until);
    }
}
