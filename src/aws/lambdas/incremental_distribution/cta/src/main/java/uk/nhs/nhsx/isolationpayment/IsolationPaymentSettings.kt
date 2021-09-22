package uk.nhs.nhsx.isolationpayment

import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.bool
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.integer
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.string
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.strings
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.value
import uk.nhs.nhsx.core.aws.dynamodb.TableName

object IsolationPaymentSettings {
    val ISOLATION_TOKEN_TABLE = value("ISOLATION_PAYMENT_TOKENS_TABLE", TableName)
    val AUDIT_LOG_PREFIX = string("AUDIT_LOG_PREFIX")
    val ISOLATION_PAYMENT_WEBSITE = string("ISOLATION_PAYMENT_WEBSITE")
    val TOKEN_EXPIRY_IN_WEEKS = integer("TOKEN_EXPIRY_IN_WEEKS")
    val COUNTRIES_WHITELISTED = strings("COUNTRIES_WHITELISTED")
    val TOKEN_CREATION_ENABLED = bool("TOKEN_CREATION_ENABLED")
}
