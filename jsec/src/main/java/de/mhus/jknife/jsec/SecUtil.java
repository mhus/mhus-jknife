/*

    Copyright (C) 2002 Mike Hummel (mh@mhus.de)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

*/
package de.mhus.jknife.jsec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Security helpers based on the jdk security framework: pem handling, key parsing, aes-gcm envelopes and password based
 * key derivation.
 */
public final class SecUtil {

    static final SecureRandom RANDOM = new SecureRandom();

    static final int GCM_NONCE_BYTES = 12;
    static final int GCM_TAG_BITS = 128;
    static final int SALT_BYTES = 16;
    static final int PBKDF2_ITERATIONS = 100_000;

    private SecUtil() {
    }

    // --- encoding helpers -------------------------------------------------

    public static String toHex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }

    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
    }

    // --- pem ---------------------------------------------------------------

    /** writes a pem block with 64 character lines */
    public static String writePem(String type, byte[] encoded) {
        var base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    /**
     * Reads pem content: with BEGIN/END markers or bare base64.
     *
     * @throws IllegalArgumentException
     *             if the content is neither valid pem nor base64
     */
    public static byte[] readPem(String content) {
        if (content.contains("-----BEGIN")) {
            var matcher = java.util.regex.Pattern
                    .compile("-----BEGIN [^-]+-----(.*?)-----END [^-]+-----", java.util.regex.Pattern.DOTALL)
                    .matcher(content);
            if (!matcher.find())
                throw new IllegalArgumentException("Invalid pem: no BEGIN/END block found");
            return fromBase64(matcher.group(1));
        }
        try {
            return fromBase64(content);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Content is neither pem nor base64");
        }
    }

    // --- keys ---------------------------------------------------------------

    public static PrivateKey readPrivateKey(byte[] der) {
        for (String algorithm : new String[] { "RSA", "EC", "Ed25519" }) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(der));
            } catch (Exception ignored) {
                // try the next factory
            }
        }
        throw new IllegalArgumentException("Unsupported private key format (expected PKCS#8 RSA, EC or Ed25519)");
    }

    public static PublicKey readPublicKey(byte[] der) {
        for (String algorithm : new String[] { "RSA", "EC", "Ed25519" }) {
            try {
                return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
            } catch (Exception ignored) {
                // try the next factory
            }
        }
        throw new IllegalArgumentException("Unsupported public key format (expected X.509 RSA, EC or Ed25519)");
    }

    /** signature algorithm for the given key type */
    public static String defaultSignatureAlgorithm(Key key) {
        return switch (key.getAlgorithm().toUpperCase()) {
        case "RSA" -> "SHA256withRSA";
        case "EC" -> "SHA256withECDSA";
        case "ED25519", "ED448", "EDDSA" -> key.getAlgorithm();
        default -> throw new IllegalArgumentException(
                "No default signature algorithm for key type " + key.getAlgorithm());
        };
    }

    // --- aes-gcm envelope ---------------------------------------------------

    /**
     * Encrypts with aes-gcm. Envelope layout: salt (only for password mode) + nonce + ciphertext-with-tag.
     */
    public static byte[] encryptAesGcm(byte[] keyBytes, byte[] plaintext, byte[] salt) throws Exception {
        byte[] nonce = new byte[GCM_NONCE_BYTES];
        RANDOM.nextBytes(nonce);

        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] ciphertext = cipher.doFinal(plaintext);

        var envelope = new java.io.ByteArrayOutputStream();
        if (salt != null)
            envelope.writeBytes(salt);
        envelope.writeBytes(nonce);
        envelope.writeBytes(ciphertext);
        return envelope.toByteArray();
    }

    /**
     * Decrypts an aes-gcm envelope. If the envelope contains a salt (password mode), the expected salt length must be
     * given so the nonce can be located.
     */
    public static byte[] decryptAesGcm(byte[] keyBytes, byte[] envelope, int saltLength) throws Exception {
        if (envelope.length < saltLength + GCM_NONCE_BYTES + GCM_TAG_BITS / 8)
            throw new IllegalArgumentException("Encrypted data too short");

        byte[] nonce = new byte[GCM_NONCE_BYTES];
        System.arraycopy(envelope, saltLength, nonce, 0, GCM_NONCE_BYTES);
        byte[] ciphertext = new byte[envelope.length - saltLength - GCM_NONCE_BYTES];
        System.arraycopy(envelope, saltLength + GCM_NONCE_BYTES, ciphertext, 0, ciphertext.length);

        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        return cipher.doFinal(ciphertext);
    }

    /** derives an aes key from a password with PBKDF2WithHmacSHA256 */
    public static byte[] deriveKey(char[] password, byte[] salt, int keyBits) throws Exception {
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, keyBits);
        return factory.generateSecret(spec).getEncoded();
    }

    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
