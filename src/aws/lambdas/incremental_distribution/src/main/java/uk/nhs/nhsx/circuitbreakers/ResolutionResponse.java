package uk.nhs.nhsx.circuitbreakers;

public class ResolutionResponse {

    private String approval;

    public String getApproval() {
        return approval;
    }

    public void setApproval(String approval) {
        this.approval = approval;
    }

    @Override
    public String toString() {
        return "ResolutionResponse{" +
                "approval='" + approval + '\'' +
                '}';
    }
}
