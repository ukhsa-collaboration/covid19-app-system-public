package uk.nhs.nhsx.highriskvenuesupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HighRiskVenue {
    public final String id;
    public final RiskyWindow riskyWindow;
    public final String messageType;
    public final String optionalParameter;

    public HighRiskVenue(String id, RiskyWindow riskyWindow) {
        this(id, riskyWindow, "M1", null);
    }
    @JsonCreator
    public HighRiskVenue(String id, RiskyWindow riskyWindow, String messageType,  String optionalParameter) {
        this.id = id;
        this.riskyWindow = riskyWindow;
        this.messageType = messageType;
        this.optionalParameter = optionalParameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighRiskVenue that = (HighRiskVenue) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(riskyWindow, that.riskyWindow) &&
            Objects.equals(messageType, that.messageType) &&
            Objects.equals(optionalParameter, that.optionalParameter);
    }

    @Override
    public String toString() {
        return "HighRiskVenue{" +
            "id='" + id + '\'' +
            ", riskyWindow=" + riskyWindow +
            ", messageType='" + messageType + '\'' +
            ", optionalParameter='" + optionalParameter + '\'' +
            '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, riskyWindow, messageType, optionalParameter);
    }
}
