package uk.nhs.nhsx.core.aws.ssm

import uk.nhs.nhsx.core.signature.KeyId

class ParameterKeyLookup(
    parameters: Parameters,
    parameterName: ParameterName
) : KeyLookup {
    private val parameter = parameters.parameter(parameterName, KeyId::of)

    override fun kmsKeyId(): KeyId = parameter.value()
}
