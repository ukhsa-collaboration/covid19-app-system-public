package uk.nhs.nhsx.testhelper.http4k

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver

fun Approver.assertApproved(content: String) = assertApproved(Response(OK).body(content))