package uk.nhs.nhsx.testhelper.approvals

import org.http4k.core.HttpMessage
import org.http4k.testing.ApprovalContent
import org.http4k.testing.ApprovalContent.Companion.HttpBodyOnly
import org.http4k.testing.ApprovalFailed
import org.http4k.testing.ApprovalSource
import org.http4k.testing.Approver
import org.http4k.testing.BaseApprovalTest
import org.http4k.testing.FileSystemApprovalSource
import org.http4k.testing.TestNamer.Companion.ClassAndMethod
import org.junit.jupiter.api.extension.ExtensionContext
import strikt.api.expectThat
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import java.io.File
import java.io.InputStreamReader

class JsonNamedResourceApprover(
    private val name: String,
    private val approvalContent: ApprovalContent,
    private val approvalSource: ApprovalSource
) : Approver {

    override fun <T : HttpMessage> assertApproved(httpMessage: T) {
        val approved = approvalSource.approvedFor(name)

        with(approved.input()) {
            val actual = approvalSource.actualFor(name)

            when (this) {
                null -> with(approvalContent(httpMessage)) {
                    if (available() > 0) {
                        copyTo(actual.output())
                        throw ApprovalFailed("No approved content found", actual, approved)
                    }
                }
                else -> try {
                    expectThat(approvalContent(httpMessage).reader().readText())
                        .isEqualToJson(approvalContent(this).reader().use(InputStreamReader::readText))
                } catch (e: AssertionError) {
                    approvalContent(httpMessage).copyTo(actual.output())
                    throw AssertionError(ApprovalFailed("Mismatch", actual, approved).message + "\n" + e.message)
                }
            }
        }
    }
}

class StrictJsonApprovalTest : BaseApprovalTest {
    override fun approverFor(context: ExtensionContext): Approver = JsonNamedResourceApprover(
        ClassAndMethod.nameFor(context.requiredTestClass, context.requiredTestMethod),
        HttpBodyOnly(),
        FileSystemApprovalSource(File("src/test/resources"))
    )
}
