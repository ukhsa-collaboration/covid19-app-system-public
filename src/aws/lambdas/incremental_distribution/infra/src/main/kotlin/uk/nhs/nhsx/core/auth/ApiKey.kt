package uk.nhs.nhsx.core.auth

import uk.nhs.nhsx.core.aws.secretsmanager.SecretName

data class ApiKey(val keyName: String, val keyValue: String) {
    fun secretNameFrom(apiName: ApiName): SecretName = SecretName.of("/${apiName.keyName}/$keyName")
}
