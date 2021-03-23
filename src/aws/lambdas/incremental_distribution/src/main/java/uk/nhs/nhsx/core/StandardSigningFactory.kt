package uk.nhs.nhsx.core

import com.amazonaws.services.kms.AWSKMS
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.StandardSigning.SSM_KEY_ID_PARAMETER_NAME
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.aws.kms.KmsSigner
import uk.nhs.nhsx.core.aws.ssm.ParameterKeyLookup
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signer

class StandardSigningFactory(
    private val clock: Clock,
    private val parameters: Parameters,
    private val client: AWSKMS
) {

    fun signResponseWithKeyGivenInSsm(environment: Environment, events: Events): ResponseSigner =
        AwsResponseSigner(datedSigner(environment), events)

    fun datedSigner(environment: Environment): RFC2616DatedSigner =
        datedSigner(environment.access.required(SSM_KEY_ID_PARAMETER_NAME))

    fun datedSigner(parameterName: ParameterName): RFC2616DatedSigner =
        RFC2616DatedSigner(clock, signContentWithKeyFromParameter(parameterName))

    fun signContentWithKeyFromParameter(name: ParameterName): Signer {
        val parameterKeyLookup = ParameterKeyLookup(parameters, name)
        return KmsSigner(parameterKeyLookup::kmsKeyId, client)
    }
}

object StandardSigning {
    val SSM_KEY_ID_PARAMETER_NAME = EnvironmentKey.value("SSM_KEY_ID_PARAMETER_NAME", ParameterName)
}
