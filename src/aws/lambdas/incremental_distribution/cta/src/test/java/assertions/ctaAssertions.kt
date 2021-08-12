package assertions

import assertions.DomainAssertions.isValid
import batchZipCreation.Exposure.TemporaryExposureKey
import batchZipCreation.Exposure.TemporaryExposureKeyExport
import strikt.api.Assertion
import strikt.assertions.flatMap
import strikt.assertions.isEqualTo
import strikt.assertions.isNotBlank
import strikt.assertions.isTrue
import strikt.assertions.size
import strikt.assertions.withFirst
import strikt.assertions.withLast
import uk.nhs.nhsx.circuitbreakers.TokenResponse
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2

object VirologyAssertionsV1 {
    val Assertion.Builder<VirologyLookupResponseV1>.testResult get() = get(VirologyLookupResponseV1::testResult)
    val Assertion.Builder<VirologyLookupResponseV1>.testEndDate get() = get(VirologyLookupResponseV1::testEndDate)
    val Assertion.Builder<VirologyLookupResponseV1>.testKit get() = get(VirologyLookupResponseV1::testKit)
}

object VirologyAssertionsV2 {
    val Assertion.Builder<VirologyLookupResponseV2>.testResult get() = get(VirologyLookupResponseV2::testResult)
    val Assertion.Builder<VirologyLookupResponseV2>.testEndDate get() = get(VirologyLookupResponseV2::testEndDate)
    val Assertion.Builder<VirologyLookupResponseV2>.testKit get() = get(VirologyLookupResponseV2::testKit)
    val Assertion.Builder<VirologyLookupResponseV2>.diagnosisKeySubmissionSupported get() = get(VirologyLookupResponseV2::diagnosisKeySubmissionSupported)
    val Assertion.Builder<VirologyLookupResponseV2>.requiresConfirmatoryTest get() = get(VirologyLookupResponseV2::requiresConfirmatoryTest)
    val Assertion.Builder<VirologyLookupResponseV2>.confirmatoryDayLimit get() = get(VirologyLookupResponseV2::confirmatoryDayLimit)
}

object CtaExchangeAssertionsV1 {
    val Assertion.Builder<CtaExchangeResponseV1>.testResult get() = get(CtaExchangeResponseV1::testResult)
    val Assertion.Builder<CtaExchangeResponseV1>.testEndDate get() = get(CtaExchangeResponseV1::testEndDate)
    val Assertion.Builder<CtaExchangeResponseV1>.testKit get() = get(CtaExchangeResponseV1::testKit)
}

object CtaExchangeAssertionsV2 {
    val Assertion.Builder<CtaExchangeResponseV2>.testResult get() = get(CtaExchangeResponseV2::testResult)
    val Assertion.Builder<CtaExchangeResponseV2>.testEndDate get() = get(CtaExchangeResponseV2::testEndDate)
    val Assertion.Builder<CtaExchangeResponseV2>.testKit get() = get(CtaExchangeResponseV2::testKit)
    val Assertion.Builder<CtaExchangeResponseV2>.diagnosisKeySubmissionSupported get() = get(CtaExchangeResponseV2::diagnosisKeySubmissionSupported)
    val Assertion.Builder<CtaExchangeResponseV2>.requiresConfirmatoryTest get() = get(CtaExchangeResponseV2::requiresConfirmatoryTest)
    val Assertion.Builder<CtaExchangeResponseV2>.confirmatoryDayLimit get() = get(CtaExchangeResponseV2::confirmatoryDayLimit)
}

object CircuitBreakersAssertions {
    val Assertion.Builder<TokenResponse>.approval get() = get(TokenResponse::approval)
    val Assertion.Builder<TokenResponse>.approvalToken get() = get(TokenResponse::approvalToken)
}

object IsolationPaymentAssertions {
    fun Assertion.Builder<TokenGenerationResponse.OK>.hasValidIpcToken() = apply {
        get(TokenGenerationResponse.OK::ipcToken).isValid()
        get(TokenGenerationResponse.OK::isEnabled).isTrue()
    }
}

object DomainAssertions {
    fun Assertion.Builder<IpcTokenId>.isValid() = apply {
        get(IpcTokenId::value).isNotBlank()
    }
}

object ExposureKeyAssertions {
    val Assertion.Builder<List<TemporaryExposureKeyExport>>.keys get() = flatMap(TemporaryExposureKeyExport::getKeysList)
    val Assertion.Builder<TemporaryExposureKey>.keyData get() = get(TemporaryExposureKey::getKeyData)
    val Assertion.Builder<TemporaryExposureKey>.rollingPeriod get() = get(TemporaryExposureKey::getRollingPeriod)
}

object MetaHeaderAssertions {
    fun Assertion.Builder<List<MetaHeader>>.matchesMeta(
        keyId: KeyId,
        signature: String,
        date: String
    ) = withFirst {
        get(MetaHeader::asS3MetaName).isEqualTo("Signature")
        get(MetaHeader::value).isEqualTo("""keyId="${keyId.value}",signature="$signature"""")
    }.withLast {
        get(MetaHeader::asS3MetaName).isEqualTo("Signature-Date")
        get(MetaHeader::value).isEqualTo(date)
    }.size.isEqualTo(2)
}
