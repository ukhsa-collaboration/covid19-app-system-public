package uk.nhs.nhsx.circuitbreakers;

public class TokenResponse {

    private String approvalToken;
    private String approval;

    public String getApprovalToken() {
        return approvalToken;
    }

    public void setApprovalToken(String approvalToken) {
        this.approvalToken = approvalToken;
    }

    public String getApproval() {
        return approval;
    }

    public void setApproval(String approval) {
        this.approval = approval;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "approvalToken='" + approvalToken + '\'' +
                ", approval='" + approval + '\'' +
                '}';
    }
}
