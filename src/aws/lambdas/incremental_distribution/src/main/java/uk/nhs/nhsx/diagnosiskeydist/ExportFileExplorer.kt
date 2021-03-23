package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure.TemporaryExposureKeyExport
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import java.io.FileInputStream
import java.util.*
import kotlin.system.exitProcess

/**
 * current?
 */
object ExportFileExplorer {
    @JvmStatic
    fun main(args: Array<String>) {
        Tracing.disableXRayComplaintsForMainClasses()
        FileInputStream(if (args.isEmpty()) "export.bin" else args[0])
            .use {
                it.skip(DistributionService.EK_EXPORT_V1_HEADER.length.toLong())
                val export = TemporaryExposureKeyExport.parseFrom(it)
                println("{")
                if (export.hasRegion()) println("""  "region": "${export.region}",""")
                if (export.hasBatchNum()) println("""  "batchNum": "${export.batchNum}",""")
                if (export.hasBatchSize()) println("""  "batchSize": "${export.batchSize}",""")
                if (export.hasStartTimestamp())
                    println("""  "startTimestamp": "${export.startTimestamp}" /* ${Date(export.startTimestamp * 1000)} */,""")
                if (export.hasEndTimestamp()) println("""  "endTimestamp": "${export.endTimestamp}" /* ${Date(export.endTimestamp * 1000)} */,""")
                println("""  "temporaryExposureKeys": [""")
                val i = export.keysList.iterator()
                while (i.hasNext()) {
                    val key = i.next()
                    print("    {")
                    print(""""key":"${Base64.getEncoder().encodeToString(key.keyData.toByteArray())}",""")
                    print(
                        """"rollingStartNumber":"${key.rollingStartIntervalNumber}" /* """ +
                            ENIntervalNumber(key.rollingStartIntervalNumber.toLong()).toTimestamp() + " */,"
                    )
                    print(""""rollingPeriod":"${key.rollingPeriod}" /* ${ENIntervalNumber((key.rollingStartIntervalNumber + key.rollingPeriod).toLong()).toTimestamp()} */,""")
                    print(""""transmissionRiskLevel":"${key.transmissionRiskLevel}"""")
                    if (key.hasDaysSinceOnsetOfSymptoms()) print(""","daysSinceOnsetOfSymptoms":"${key.daysSinceOnsetOfSymptoms}"""")
                    if (i.hasNext()) println("},") else println("}")
                }
                println("  ]")
                println("}")
            }
        exitProcess(0)
    }
}
