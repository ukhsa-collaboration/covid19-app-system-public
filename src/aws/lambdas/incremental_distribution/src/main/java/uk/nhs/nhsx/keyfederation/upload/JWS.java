package uk.nhs.nhsx.keyfederation.upload;

import org.jose4j.base64url.SimplePEMEncoder;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class JWS {

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    private final PrivateKey privateKey;

    public JWS(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public JWS(String privateKeyAsPem) {
        try {
            this.privateKey = fromPemEncodedPrivateKey(privateKeyAsPem);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String compactSignedPayload(String payload) throws JoseException {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(payload);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        jws.setKey(privateKey);
        return jws.getCompactSerialization();
    }

    private PrivateKey fromPemEncodedPrivateKey(String pem) throws InvalidKeySpecException, NoSuchAlgorithmException {
        int beginIndex = pem.indexOf(BEGIN_PRIVATE_KEY) + BEGIN_PRIVATE_KEY.length();
        int endIndex = pem.indexOf(END_PRIVATE_KEY);
        String base64 = pem.substring(beginIndex, endIndex).trim();
        byte[] decode = SimplePEMEncoder.decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
        KeyFactory  kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }
}
