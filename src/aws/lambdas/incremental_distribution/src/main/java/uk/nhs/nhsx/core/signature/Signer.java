package uk.nhs.nhsx.core.signature;

public interface Signer {
    Signature sign(byte[] bytes);
}
