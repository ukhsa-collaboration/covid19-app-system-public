package uk.nhs.nhsx.pubdash

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DataExportHandlerTest {

    @Test
    fun `runs export and succeeds`() {
        val service = mockk<DataExportService> {
            every { export() } just Runs
        }
        val handler = DataExportHandler(environment = TestEnvironments.environmentWith(), service = service)
        val response = handler.handleRequest(mockk(), mockk())

        assertThat(response).isEqualTo(DataExported.toString())
        verify(exactly = 1) { service.export() }
    }
}
