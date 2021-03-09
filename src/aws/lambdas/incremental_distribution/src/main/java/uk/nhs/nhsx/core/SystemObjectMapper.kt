package uk.nhs.nhsx.core

import com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.headers.MobileOSVersion
import uk.nhs.nhsx.highriskvenuesupload.model.MessageType
import uk.nhs.nhsx.highriskvenuesupload.model.VenueId
import uk.nhs.nhsx.keyfederation.BatchTag
import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType
import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.virology.IpcTokenId
import uk.nhs.nhsx.virology.TestResultPollingToken
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.Instant
import java.util.*
import uk.nhs.nhsx.core.aws.ssm.ParameterName as NhsParameterName

object SystemObjectMapper {
    @JvmField
    val MAPPER: ObjectMapper = ObjectMapper()
        .deactivateDefaultTyping()
        .registerModule(ParameterNamesModule(PROPERTIES))
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .registerModule(SimpleModule().apply {
            text(Instant::toString, Instant::parse)
            text(Duration::toString)
            text(UUID::toString)
            value(ObjectKey)
            value(BucketName)
            value(BatchTag)
            value(Country)
            value(CtaToken)
            value(EnvironmentName)
            value(NhsParameterName)
            value(IpcTokenId)
            value(SecretName)
            value(TestResultPollingToken)
            value(DiagnosisKeySubmissionToken)
            value(MobileOSVersion)
            value(TestEndDate)
            value(VenueId)
            value(MessageType)
            text(TestResult::wireValue, TestResult::from)
            int(TestType::wireValue, TestType::from)
            int(ReportType::wireValue, ReportType::from)
            text(Class<*>::getSimpleName)
            text { e: EventCategory -> e.name.toUpperCase() }
            text<Throwable> {
                StringWriter()
                    .use { output ->
                        PrintWriter(output)
                            .use { printer ->
                                it.printStackTrace(printer)
                                output.toString()
                            }
                    }
            }
        })
        .configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
        .configure(FAIL_ON_NULL_CREATOR_PROPERTIES, true)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(FAIL_ON_IGNORED_PROPERTIES, true)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, false)
        .configure(FAIL_ON_READING_DUP_TREE_KEY, true)
        .configure(FAIL_ON_TRAILING_TOKENS, true)
        .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, false)

    @JvmField
    val STRICT_MAPPER: ObjectMapper = MAPPER
        .copy()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
}

private inline fun <reified T> SimpleModule.text(crossinline outFn: (T) -> String) {
    addSerializer(T::class.java, object : JsonSerializer<T>() {
        override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(outFn(value))
        }
    })
}

private inline fun <reified T : Value<*>> SimpleModule.value(vf : ValueFactory<T, *>) = text(vf::show, vf::parse)

private inline fun <reified T> SimpleModule.text(crossinline outFn: (T) -> String, crossinline inFn: (String) -> T) {
    text(outFn)
    addDeserializer(T::class.java, object : JsonDeserializer<T>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = inFn(p.text)
    })
}

private inline fun <reified T> SimpleModule.int(crossinline outFn: (T) -> Int) {
    addSerializer(T::class.java, object : JsonSerializer<T>() {
        override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeNumber(outFn(value))
        }
    })
}

private inline fun <reified T> SimpleModule.int(crossinline outFn: (T) -> Int, crossinline inFn: (Int) -> T) {
    int(outFn)
    addDeserializer(T::class.java, object : JsonDeserializer<T>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = inFn(p.intValue)
    })
}

