package uk.nhs.nhsx.core.aws.xray

import com.amazonaws.xray.AWSXRay
import com.amazonaws.xray.AWSXRayRecorderBuilder

object Tracing {
    @JvmStatic
    fun disableXRayComplaintsForMainClasses() {
        AWSXRay.setGlobalRecorder(
            AWSXRayRecorderBuilder.standard()
                .withContextMissingStrategy { _, _ -> }
                .build()
        )
    }

    fun <T> time(name: String = "", print: (String) -> Unit = ::println, function: () -> T): T {
        val start = System.currentTimeMillis()
        return try {
            function()
        } finally {
            print("$name ${System.currentTimeMillis() - start}")
        }
    }
}
