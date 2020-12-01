package uk.nhs.nhsx.core.aws.ssm;

import org.junit.jupiter.api.Test;
import uk.nhs.nhsx.core.signature.KeyId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


public class KeyIdTest {

	@Test
	public void testKeyId() {
		assertThat(
			KeyId.of("arn:aws:kms:eu-west-2:1234567890:key/b4c27bf3-8a76-4d2b-b91c-2152e7710a57").value,
			equalTo("b4c27bf3-8a76-4d2b-b91c-2152e7710a57")
		);
		assertThat(KeyId.of("b4c27bf3-8a76-4d2b-b91c-2152e7710a57").value, equalTo("b4c27bf3-8a76-4d2b-b91c-2152e7710a57"));
	}
}
