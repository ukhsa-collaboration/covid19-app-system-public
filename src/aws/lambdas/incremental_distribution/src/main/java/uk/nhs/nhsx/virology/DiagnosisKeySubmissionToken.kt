package uk.nhs.nhsx.virology

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class DiagnosisKeySubmissionToken private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<DiagnosisKeySubmissionToken>(::DiagnosisKeySubmissionToken)
}
