package smoke.actors

import uk.nhs.nhsx.core.ValueType

sealed class MobileOS {
    object iOS : MobileOS()
    object Android : MobileOS()
}

enum class TestResult {
    POSITIVE, NEGATIVE, VOID
}

class MobileDeviceModel(value: String) : ValueType<MobileDeviceModel>(value)
class TestResultPollingToken(value: String) : ValueType<TestResultPollingToken>(value)
class DiagnosisKeySubmissionToken(value: String) : ValueType<DiagnosisKeySubmissionToken>(value)
class IpcToken(value: String) : ValueType<IpcToken>(value)

sealed class UserCountry(value: String) : ValueType<UserCountry>(value) {
    object England : UserCountry("England")
    object Wales : UserCountry("Wales")
    class Other(value: String) : UserCountry(value)
}

sealed class ApiVersion {
    object V1 : ApiVersion()
    object V2 : ApiVersion()
}
