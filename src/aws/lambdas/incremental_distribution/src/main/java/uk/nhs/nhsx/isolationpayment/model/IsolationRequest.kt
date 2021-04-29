package uk.nhs.nhsx.isolationpayment.model

import uk.nhs.nhsx.domain.IpcTokenId

data class IsolationRequest(val ipcToken: IpcTokenId) {
    val contractVersion = 1
}
