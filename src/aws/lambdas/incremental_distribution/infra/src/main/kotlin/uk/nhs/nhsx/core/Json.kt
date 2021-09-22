package uk.nhs.nhsx.core

import com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.EventCategory.Operational
import uk.nhs.nhsx.core.events.EventEnvelope
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.headers.MobileOSVersion
import uk.nhs.nhsx.domain.MessageType
import uk.nhs.nhsx.domain.OptionalHighRiskVenueParam
import uk.nhs.nhsx.domain.PostDistrict
import uk.nhs.nhsx.domain.RiskIndicator
import uk.nhs.nhsx.domain.TierIndicator
import uk.nhs.nhsx.domain.VenueId
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.KClass

object Json {
    init {
        // Kickstart Jackson's ObjectMapper initialisation in a different thread.
        //
        // Creating a new ObjectMapper can take up to 2s and by doing the initialisation
        // in a different thread reduces the cold start times of the lambdas.
        thread {
            AppServicesJson.asFormatString(
                EventEnvelope(
                    mapOf(
                        "category" to Operational,
                        "name" to "ObjectMapperWarmUp",
                        "timestamp" to Instant.now(Clock.systemUTC()),
                    ), InfoEvent("Done")
                )
            )
        }
    }

    inline fun <reified T : Any> readJsonOrThrow(inputStream: InputStream?): T = readJsonOrThrow(inputStream, T::class)

    fun <T : Any> readJsonOrThrow(inputStream: InputStream?, clazz: KClass<T>): T =
        AppServicesJson.asA(inputStream ?: "".byteInputStream(), clazz)

    inline fun <reified T : Any> readJsonOrThrow(value: String?): T = readJsonOrThrow(value, T::class)

    fun <T : Any> readJsonOrThrow(value: String?, clazz: KClass<T>): T = AppServicesJson.asA(value ?: "", clazz)

    inline fun <reified T : Any> readJsonOrNull(value: String?): T? = try {
        readJsonOrThrow(value)
    } catch (e: Exception) {
        null
    }

    inline fun <reified T : Any> readStrictOThrow(value: String?): T = StrictAppServicesJson.asA(value ?: "", T::class)

    inline fun <reified T : Any> readStrictOrNull(value: String?): T? = try {
        readStrictOThrow(value)
    } catch (e: Exception) {
        null
    }

    fun toJson(value: Any?): String = AppServicesJson.asFormatString(value ?: Unit)
}

inline fun <reified T : Any> Json.readJsonOrNull(value: String?, handleError: (Exception) -> Unit): T? = try {
    readJsonOrThrow(value)
} catch (e: Exception) {
    handleError(e)
    null
}

inline fun <reified T : Any> Json.readStrictOrNull(
    value: String?,
    handleError: (Exception) -> Unit
): T? = try {
    readStrictOThrow(value)
} catch (e: Exception) {
    handleError(e)
    null
}

object AppServicesJson : ConfigurableJackson(
    KotlinModule(strictNullChecks = true)
        .asConfigurable()
        .domainMappings()
        .done()
        .deactivateDefaultTyping()
        .registerModule(ParameterNamesModule(PROPERTIES))
        .configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
        .configure(FAIL_ON_NULL_CREATOR_PROPERTIES, true)
        .configure(FAIL_ON_IGNORED_PROPERTIES, true)
        .configure(FAIL_ON_READING_DUP_TREE_KEY, true)
        .configure(FAIL_ON_TRAILING_TOKENS, true)
        .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, false)
        .configure(USE_BIG_INTEGER_FOR_INTS, false)
)

object StrictAppServicesJson : ConfigurableJackson(
    AppServicesJson.mapper.copy().configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
)

fun AutoMappingConfiguration<ObjectMapper>.domainMappings() =
    withStandardMappings()
        .value(ObjectKey)
        .value(BatchTag)
        .value(BucketName)
        .value(Country)
        .value(CtaToken)
        .value(DiagnosisKeySubmissionToken)
        .value(EnvironmentName)
        .value(IpcTokenId)
        .value(PostDistrict)
        .value(MessageType)
        .value(MobileOSVersion)
        .value(OptionalHighRiskVenueParam)
        .value(ParameterName)
        .value(PostDistrict)
        .value(SecretName)
        .value(TestEndDate)
        .value(TestResultPollingToken)
        .value(TierIndicator)
        .value(VenueId)
        .text(BiDiMapping(TestResult::from, TestResult::wireValue))
        .text(BiDiMapping(RiskIndicator::from, RiskIndicator::wireValue))
        .int(BiDiMapping(TestType::from, TestType::wireValue))
        .int(BiDiMapping(ReportType::from, ReportType::wireValue))
        .text(BiDiMapping({ error("illegal") }, Class<*>::getSimpleName))
        .text(BiDiMapping({ error("illegal") }, { e: EventCategory -> e.name.uppercase(Locale.getDefault()) }))
