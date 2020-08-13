package uk.nhs.nhsx.core.aws.s3;

public class MetaHeader {
    private final String key;
    public final String value;

    public MetaHeader(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "MetaHeader{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }

    public String asHttpHeaderName() {
        return "X-Amz-Meta-" + key;
    }

    public String asS3MetaName() {
        return key;
    }
}
