@file:Suppress("unused")

package uk.nhs.nhsx.localstats

import com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import uk.nhs.nhsx.localstats.domain.AreaCode
import uk.nhs.nhsx.localstats.domain.AreaName
import uk.nhs.nhsx.localstats.domain.AreaTypeCode
import uk.nhs.nhsx.localstats.domain.MetricName

object LocalStatsJson : ConfigurableJackson(
    KotlinModule(strictNullChecks = true)
        .asConfigurable()
        .domainMappings()
        .done()
        .deactivateDefaultTyping()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, true)
        .configure(WRITE_BIGDECIMAL_AS_PLAIN, true)
)

private fun AutoMappingConfiguration<ObjectMapper>.domainMappings() =
    withStandardMappings()
        .value(AreaTypeCode)
        .value(AreaCode)
        .value(AreaName)
        .value(MetricName)
