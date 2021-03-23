package uk.nhs.nhsx.diagnosiskeyssubmission.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

data class StoredTemporaryExposureKey(
    val key: String,
    val rollingStartNumber: Int,
    val rollingPeriod: Int,
    val transmissionRisk: Int,
    @JsonInclude(NON_NULL) val daysSinceOnsetOfSymptoms: Int? = null
)
