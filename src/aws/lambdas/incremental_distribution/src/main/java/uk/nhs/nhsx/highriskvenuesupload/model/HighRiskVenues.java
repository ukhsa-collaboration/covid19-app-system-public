package uk.nhs.nhsx.highriskvenuesupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class HighRiskVenues {

    public final List<HighRiskVenue> venues;

    @JsonCreator
    public HighRiskVenues(List<HighRiskVenue> venues) {
        this.venues = venues;
    }

}
