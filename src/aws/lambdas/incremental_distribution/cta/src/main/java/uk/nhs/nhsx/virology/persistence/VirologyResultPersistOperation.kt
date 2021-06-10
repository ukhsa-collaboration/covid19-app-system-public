package uk.nhs.nhsx.virology.persistence

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.HttpResponses

abstract class VirologyResultPersistOperation {
    abstract fun toHttpResponse(): APIGatewayProxyResponseEvent

    class Success : VirologyResultPersistOperation() {
        override fun toHttpResponse() = HttpResponses.accepted("successfully processed")
    }

    class TransactionFailed : VirologyResultPersistOperation() {
        override fun toHttpResponse() = HttpResponses.conflict()
    }

    class OrderNotFound : VirologyResultPersistOperation() {
        override fun toHttpResponse() = HttpResponses.badRequest()
    }
}
