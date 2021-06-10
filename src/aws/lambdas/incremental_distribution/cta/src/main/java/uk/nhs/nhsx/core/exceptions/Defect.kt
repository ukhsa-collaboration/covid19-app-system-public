package uk.nhs.nhsx.core.exceptions

import java.lang.RuntimeException

class Defect : RuntimeException {
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(message: String?) : super(message)
}
