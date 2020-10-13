package uk.nhs.nhsx.keyfederation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName

class KeyFederationConfigTest {

    @Test
    fun `valid regions`() {
        var config = makeConfig("")
        assertThat(config.validRegions).isNotNull
        assertThat(config.validRegions).isEmpty()

        config = makeConfig(null)
        assertThat(config.validRegions).isNotNull
        assertThat(config.validRegions).isEmpty()

        config = makeConfig("ABC")
        assertThat(config.validRegions).isNotNull
        assertThat(config.validRegions).containsExactly("ABC")

        config = makeConfig("ABC,DEF")
        assertThat(config.validRegions).isNotNull
        assertThat(config.validRegions).containsExactly("ABC", "DEF")
    }

    private fun makeConfig(validRegionsCsv: String?): KeyFederationConfig {
        return KeyFederationConfig(
            true,
            true,
            BucketName.of("bucket"),
            "http://localhost", SecretName.of("secret"),
            SecretName.of("secret"),
            "prefix",
            "tableName",
            validRegionsCsv,
            "GB-EAW"
        )
    }
}