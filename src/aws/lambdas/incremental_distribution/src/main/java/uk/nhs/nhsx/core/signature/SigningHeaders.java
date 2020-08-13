package uk.nhs.nhsx.core.signature;

import uk.nhs.nhsx.core.aws.s3.MetaHeader;

import java.util.ArrayList;
import java.util.List;

public class SigningHeaders {

    public static MetaHeader[] fromDatedSignature(DatedSignature dated) {

        List<MetaHeader> headers = new ArrayList<>();

        headers.add(from(dated.signature));
        headers.add(new MetaHeader("Signature-Date", dated.signatureDate.string));

        return headers.toArray(new MetaHeader[]{});
    }


    private static MetaHeader from(Signature signature) {
        return new MetaHeader(
            "Signature",
            "keyId=\"" + signature.keyId.value + "\",signature=\"" + signature.asBase64Encoded() + "\""
        );
    }

}
