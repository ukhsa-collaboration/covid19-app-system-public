package contract.infra

import org.http4k.core.Uri
import org.http4k.servirtium.InteractionStorage.Companion.Disk
import org.http4k.servirtium.ServirtiumServer
import org.http4k.servirtium.ServirtiumServer.Companion.Recording
import org.http4k.servirtium.ServirtiumServer.Companion.Replay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File

abstract class RecordingTest : BackendContractScenario {
    override lateinit var control: ServirtiumServer

    override fun outOfBandAction(call: () -> Unit) = call()

    @BeforeEach
    fun start(info: TestInfo) {
        control = Recording(
            info.markdownName(),
            Uri.of("http://unused"),
            Disk(File("src/test/resources/contract")),
            MobileAppInteractionOptions(),
            proxyClient = BackendReverseProxyHttpClient(envConfig)
        )
        control.start()
    }

    @AfterEach
    fun stop() {
        control.stop()
    }
}

abstract class ReplayTest : BackendContractScenario {
    override lateinit var control: ServirtiumServer

    override fun outOfBandAction(call: () -> Unit) {
        // do nothing here because we are faking external calls
    }

    @BeforeEach
    fun start(info: TestInfo) {
        control = Replay(
            info.markdownName(),
            Disk(File("src/test/resources/contract")),
            MobileAppInteractionOptions(),
        )
        control.start()
    }

    @AfterEach
    fun stop() {
        control.stop()
    }
}

private fun TestInfo.markdownName() = displayName.removeSuffix("()")
