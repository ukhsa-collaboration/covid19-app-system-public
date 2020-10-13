package uk.nhs.nhsx.diagnosiskeyssubmission;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.junit.Test;
import uk.nhs.nhsx.TestData;
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DiagnosisKeysSubmissionServiceTest {

    private final BucketName BUCKET_NAME = BucketName.of("some-bucket-name");
    private final String uuid = "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43";
    private final FakeS3Storage s3Storage = new FakeS3Storage();
    private final AwsDynamoClient awsDynamoClient = mock(AwsDynamoClient.class);
    private final ObjectKeyNameProvider objectKeyNameProvider = mock(ObjectKeyNameProvider.class);
    private final String tableName = "some-table-name";
    private final String hashKey = "diagnosisKeySubmissionToken";
    private final DiagnosisKeysSubmissionService service = new DiagnosisKeysSubmissionService(s3Storage, awsDynamoClient, objectKeyNameProvider, tableName, BUCKET_NAME);
    private final ObjectKey objectKey = ObjectKey.of("some-object-key");

    private final Item dynamoItem = Item.fromJSON("{\"" + hashKey + "\": \"" + uuid + "\"}");

    @Test
    public void acceptsTemporaryExposureKeys() throws IOException {
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem(tableName, hashKey, uuid)).thenReturn(dynamoItem);

        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            asList(
                new ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144),
                new ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144)
            )
        );

        service.acceptTemporaryExposureKeys(payload);
        verifyHappyPath(TestData.STORED_KEYS_PAYLOAD);
    }

    @Test
    public void acceptsTemporaryExposureKeysWithRiskLevel() throws IOException {
        ClientTemporaryExposureKey key1 = new ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144);
        ClientTemporaryExposureKey key2 = new ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144);
        key1.setTransmissionRiskLevel(5);
        key2.setTransmissionRiskLevel(4);
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem(tableName, hashKey, uuid)).thenReturn(dynamoItem);

        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            asList(key1, key2)
        );

        service.acceptTemporaryExposureKeys(payload);
        verifyHappyPath(TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL);
    }

    @Test
    public void ifMoreThanFourteenExposureKeysThenReject()  {

        ClientTemporaryExposureKey key1 = new ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144);
        List<ClientTemporaryExposureKey> listOfKeys = IntStream.range(0, 15).mapToObj(i -> key1).collect(Collectors.toList());

        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
                UUID.fromString(uuid),
                listOfKeys

        );
        service.acceptTemporaryExposureKeys(payload);
        verifyNoInteractions();

    }

    @Test
    public void acceptIfAtleastOneValidKey() throws IOException  {
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem(tableName, hashKey, uuid)).thenReturn(dynamoItem);

        ClientTemporaryExposureKey key1 = new ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144);
        ClientTemporaryExposureKey key2 = new ClientTemporaryExposureKey(null, 12345, 148);


        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            List.of(key1,key2)

        );

        service.acceptTemporaryExposureKeys(payload);
        verifyHappyPath(TestData.STORED_KEYS_PAYLOAD_ONE_KEY);

    }
    @Test
    public void rejectIfNoValidKeys() {
        ClientTemporaryExposureKey key2 = new ClientTemporaryExposureKey(null, 12345, 148);


        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            List.of(key2)

        );

        service.acceptTemporaryExposureKeys(payload);
        verifyNoInteractions();
    }

    @Test
    public void ifTokenDoesNotMatchThenKeysAreNotStored() {
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem(tableName, hashKey, uuid)).thenReturn(null);

        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            asList(
                new ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144),
                new ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144)
            )
        );

        service.acceptTemporaryExposureKeys(payload);

        verify(awsDynamoClient, times(1)).getItem(tableName, hashKey, uuid);

        assertThat(s3Storage.count, equalTo(0));
        verify(awsDynamoClient, times(0)).deleteItem(any(), any(), any());
    }

    @Test
    public void keyMustBeNonNull() {
        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            singletonList(
                new ClientTemporaryExposureKey(null, 12345, 144)
            )
        );

        service.acceptTemporaryExposureKeys(payload);

        verifyNoInteractions();
    }

    @Test
    public void keyMustBeBase64Encoded() {
        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            singletonList(
                new ClientTemporaryExposureKey("some-key", 12499, 144)
            )
        );

        service.acceptTemporaryExposureKeys(payload);

        verifyNoInteractions();
    }

    @Test
    public void keyMustBeLessThan32Bytes(){
        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            singletonList(
                new ClientTemporaryExposureKey("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==", 12499, 144)
            )
        );

        service.acceptTemporaryExposureKeys(payload);

        verifyNoInteractions();
    }

    @Test
    public void rollingStartNumberMustBeNonNegative() {
        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            singletonList(
                new ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", -1, 144)
            )
        );

        service.acceptTemporaryExposureKeys(payload);

        verifyNoInteractions();
    }

    @Test
    public void rollingPeriodNumberMustBeExactly144() {
        ClientTemporaryExposureKeysPayload payload = new ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            singletonList(
                new ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 143)
            )
        );

        service.acceptTemporaryExposureKeys(payload);

        verifyNoInteractions();
    }

    private void verifyNoInteractions() {
        verify(objectKeyNameProvider, times(0)).generateObjectKeyName();
        verify(awsDynamoClient, times(0)).getItem(any(), any(), any());
        assertThat(s3Storage.count, equalTo(0));
        verify(awsDynamoClient, times(0)).deleteItem(any(), any(), any());

        verifyNoMoreInteractions(objectKeyNameProvider);
        verifyNoMoreInteractions(awsDynamoClient);
    }

    private void verifyHappyPath(String expectedStoredPayload) throws IOException {
        verify(objectKeyNameProvider, times(1)).generateObjectKeyName();
        verify(awsDynamoClient, times(1)).getItem(tableName, hashKey, uuid);

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(expectedStoredPayload));
        verify(awsDynamoClient, times(1)).deleteItem(tableName, hashKey, uuid);

        verifyNoMoreInteractions(objectKeyNameProvider);
        verifyNoMoreInteractions(awsDynamoClient);
    }

}