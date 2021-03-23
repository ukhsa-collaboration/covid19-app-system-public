package uk.nhs.nhsx.diagnosiskeyssubmission.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

data class ClientTemporaryExposureKey(
    val key: String?,
    val rollingStartNumber: Int,
    val rollingPeriod: Int,
    var transmissionRiskLevel: Int = 7,
    @JsonInclude(NON_NULL) var daysSinceOnsetOfSymptoms: Int? = null
)
