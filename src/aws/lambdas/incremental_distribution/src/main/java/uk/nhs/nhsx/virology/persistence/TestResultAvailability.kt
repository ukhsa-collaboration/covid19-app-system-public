package uk.nhs.nhsx.virology.persistence

internal enum class TestResultAvailability(val text: String) {
    AVAILABLE("available"), PENDING("pending");
}
