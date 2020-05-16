package consensus.crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class ECCCipher {
    private ECCCipher() {
    }

    private static final KeyPairGenerator keyGen;
    private static final KeyFactory keyFactory;

    static {
        try {
            keyGen = KeyPairGenerator.getInstance("EC");
            keyFactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing EC support");
        }
    }

    public static String publicKeyToHex(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        return StringUtils.byteArrayToHex(encoded);
    }

    public static PublicKey hexToPublicKey(String hex) throws Exception {
        byte[] encoded = StringUtils.hexToByteArray(hex);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static KeyPair generateKeyPair() {
        try {
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Failed to generate a pair of keys");
        }
    }

    public static byte[] sign(PrivateKey privateKey, String message) throws Exception {
        Signature signature = Signature.getInstance("SHA1withECDSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        return signature.sign();
    }

    public static boolean verifySignature(PublicKey publicKey, byte[] signature, String message) throws Exception {
        Signature signatureVerifier = Signature.getInstance("SHA1withECDSA");
        signatureVerifier.initVerify(publicKey);
        signatureVerifier.update(message.getBytes());
        return signatureVerifier.verify(signature);
    }
}
