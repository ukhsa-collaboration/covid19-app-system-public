package uk.nhs.nhsx.core.signature

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromFile
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.xray.Tracing.disableXRayComplaintsForMainClasses
import uk.nhs.nhsx.core.signature.SigningHeaders.fromDatedSignature
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

/**
 * This class is used during the deployment to generate signatures for the S3 static files
 */
object DistributionSignatureMain {

    @JvmStatic
    fun main(args: Array<String>) {
        disableXRayComplaintsForMainClasses()

        val commandLine = CommandLine()
        val commander = JCommander.newBuilder().addObject(commandLine).build()

        commander.parse(*args)

        if (commandLine.help) {
            commander.usage()
            exitProcess(1)
        }

        sign(
            StandardSigningFactory(
                SystemClock.CLOCK,
                AwsSsmParameters(),
                AWSKMSClientBuilder.defaultClient()
            ).datedSigner(
                ParameterName.of(
                    commandLine.ssmKeyId
                )
            ), fromFile(File(commandLine.input)), outputFile(commandLine.output)
        )

        exitProcess(0)
    }

    fun sign(
        signer: DatedSigner,
        source: ByteArraySource,
        output: PrintStream
    ) {
        val signature = signer.sign(DistributionSignature(source))

        val signed = fromDatedSignature(signature).associateBy({ it.asS3MetaName() }, { it.value })

        output.use { SystemObjectMapper.MAPPER.writeValue(it, signed) }
    }

    private fun outputFile(output: String?): PrintStream = if (output == null) {
        System.out
    } else {
        PrintStream(FileOutputStream(output), false, StandardCharsets.UTF_8)
    }

    class CommandLine {
        @Parameter(
            names = ["--input"],
            required = true,
            description = "Path To Input Fie"
        )
        lateinit var input: String

        @Parameter(
            names = ["--output"],
            description = "File to output to (default stdout)"
        )
        var output: String? = null

        @Parameter(
            names = ["--ssm-key-id"],
            required = true,
            description = "Name to look up in SSM to get Key Id"
        )
        lateinit var ssmKeyId: String

        @Parameter(
            names = ["--help"],
            help = true,
            description = "Show help"
        )
        var help = false
    }
}
