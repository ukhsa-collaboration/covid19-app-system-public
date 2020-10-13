package uk.nhs.nhsx.highriskpostcodesupload

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.InputStreamReader


class RiskPostCodesV2Test {

    @Test
    fun `deserialize json`() {
        val resourceAsStream = this.javaClass.getResourceAsStream("/highriskpostcodesupload/metadata.json")
        assertThat(resourceAsStream).isNotNull()
        val reader = InputStreamReader(resourceAsStream)
        val riskLevels = ObjectMapper().readValue(reader, object : TypeReference<Map<String, RiskLevel>>() {})
        assertThat(riskLevels.keys).contains("Tier 1", "Tier 2", "Tier 3")
    }

}