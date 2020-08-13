package uk.nhs.nhsx.highriskvenuesupload.model;

import java.util.Objects;

public class HighRiskVenue {
    public final String id;
    public final RiskyWindow riskyWindow;

    public HighRiskVenue(String id, RiskyWindow riskyWindow) {
        this.id = id;
        this.riskyWindow = riskyWindow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighRiskVenue that = (HighRiskVenue) o;
        return id.equals(that.id) &&
                riskyWindow.equals(that.riskyWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, riskyWindow);
    }
}
