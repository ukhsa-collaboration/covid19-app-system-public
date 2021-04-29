package uk.nhs.nhsx.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class DiagnosisKeySubmissionToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<DiagnosisKeySubmissionToken>(::DiagnosisKeySubmissionToken)
}
