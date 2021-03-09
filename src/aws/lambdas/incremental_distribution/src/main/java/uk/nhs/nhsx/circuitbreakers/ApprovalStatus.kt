package uk.nhs.nhsx.circuitbreakers

enum class ApprovalStatus(val statusName: String) {
    YES("yes"), NO("no"), PENDING("pending");
}
