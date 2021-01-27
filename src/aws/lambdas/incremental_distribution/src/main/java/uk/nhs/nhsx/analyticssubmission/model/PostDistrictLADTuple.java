package uk.nhs.nhsx.analyticssubmission.model;

import java.util.Objects;

public class PostDistrictLADTuple {

    public final String postDistrict;
    public final String localAuthorityId;

    public PostDistrictLADTuple(String postDistrict, String localAuthorityId) {
        this.postDistrict = postDistrict;
        this.localAuthorityId = localAuthorityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostDistrictLADTuple that = (PostDistrictLADTuple) o;
        return Objects.equals(postDistrict, that.postDistrict) && Objects.equals(localAuthorityId, that.localAuthorityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postDistrict, localAuthorityId);
    }

    @Override
    public String toString() {
        return "PostDistrictLADTuple{" +
            "postDistrict='" + postDistrict + '\'' +
            ", localAuthorityId='" + localAuthorityId + '\'' +
            '}';
    }
}
