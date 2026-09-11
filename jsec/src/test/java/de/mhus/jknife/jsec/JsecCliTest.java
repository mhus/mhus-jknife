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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsecCliTest {

    @TempDir
    Path tempDir;

    private record Result(int exitCode, String out, String err) {
    }

    private Result run(String... args) {
        var outBuffer = new ByteArrayOutputStream();
        var errBuffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        var oldErr = System.err;
        try {
            System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JsecCmd()).execute(args);
            return new Result(exit, new String(outBuffer.toByteArray(), StandardCharsets.UTF_8),
                    new String(errBuffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    // --- hash -----------------------------------------------------------------

    @Test
    void hashKnownVectors() {
        // sha256("hello") well known vector
        var sha = run("hash", "-a", "sha256", "hello");
        assertThat(sha.exitCode()).isZero();
        assertThat(sha.out().trim()).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");

        // default algorithm is sha256
        assertThat(run("hash", "hello").out().trim())
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");

        var md5 = run("hash", "-a", "md5", "hello");
        assertThat(md5.out().trim()).isEqualTo("5d41402abc4b2a76b9719d911017c592");

        var sha512 = run("hash", "-a", "sha512", "hello");
        assertThat(sha512.out().trim()).startsWith("9b71d224bd62f3785d96d4");
    }

    @Test
    void hashBase64OutputAndStdin() throws Exception {
        var oldIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
            var r = run("hash", "-o", "base64");
            assertThat(r.exitCode()).isZero();
            // base64 of the sha256 digest of "hello"
            assertThat(r.out().trim()).isEqualTo("LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=");
        } finally {
            System.setIn(oldIn);
        }
    }

    @Test
    void hashListAndInvalid() {
        var list = run("hash", "--list");
        assertThat(list.exitCode()).isZero();
        assertThat(list.out()).contains("SHA-256").contains("MD5");

        assertThat(run("hash", "-a", "no-such-alg", "x").exitCode()).isEqualTo(2);
        assertThat(run("hash", "-o", "nope", "x").exitCode()).isEqualTo(2);
    }

    // --- create secret + encrypt/decrypt ----------------------------------------

    @Test
    void encryptDecryptWithSecretKey() throws Exception {
        var secret = run("create", "secret", "-s", "256");
        assertThat(secret.exitCode()).isZero();
        String key = secret.out().trim();
        assertThat(java.util.Base64.getDecoder().decode(key)).hasSize(32);

        var encrypted = run("encrypt", "--key", key, "top secret message");
        assertThat(encrypted.exitCode()).isZero();
        assertThat(encrypted.out().trim()).isNotEqualTo("top secret message").isNotBlank();

        var decrypted = run("decrypt", "--key", key, encrypted.out().trim());
        assertThat(decrypted.exitCode()).isZero();
        assertThat(decrypted.out()).isEqualTo("top secret message");
    }

    @Test
    void encryptDecryptWithPassword() {
        var encrypted = run("encrypt", "--password", "geheim", "data!");
        assertThat(encrypted.exitCode()).isZero();

        var decrypted = run("decrypt", "--password", "geheim", encrypted.out().trim());
        assertThat(decrypted.exitCode()).isZero();
        assertThat(decrypted.out()).isEqualTo("data!");

        // wrong password fails
        assertThat(run("decrypt", "--password", "wrong", encrypted.out().trim()).exitCode()).isEqualTo(2);
    }

    @Test
    void encryptDecryptWithKeyFile() throws Exception {
        var secret = run("create", "secret", "-s", "128");
        var keyFile = tempDir.resolve("secret.b64");
        Files.writeString(keyFile, secret.out().trim());

        var encrypted = run("encrypt", "--key-file", keyFile.toString(), "file key");
        assertThat(encrypted.exitCode()).isZero();

        var decrypted = run("decrypt", "--key-file", keyFile.toString(), encrypted.out().trim());
        assertThat(decrypted.out()).isEqualTo("file key");
    }

    @Test
    void encryptInvalidKeySize() {
        // 64 bit key is no valid aes size
        var secret = run("create", "secret", "-s", "64");
        assertThat(run("encrypt", "--key", secret.out().trim(), "x").exitCode()).isEqualTo(2);
        // no key at all
        assertThat(run("encrypt", "x").exitCode()).isEqualTo(2);
    }

    // --- create key + sign/verify -------------------------------------------------

    @Test
    void signVerifyRsaRoundtrip() throws Exception {
        var privateFile = tempDir.resolve("private.pem");
        var publicFile = tempDir.resolve("public.pem");
        var created = run("create", "key", "-a", "rsa", "-s", "2048", "--out-private", privateFile.toString(),
                "--out-public", publicFile.toString());
        assertThat(created.exitCode()).isZero();
        assertThat(created.out()).contains("private key written").contains("public key written");
        assertThat(Files.readString(privateFile)).contains("-----BEGIN PRIVATE KEY-----");
        assertThat(Files.readString(publicFile)).contains("-----BEGIN PUBLIC KEY-----");

        var signed = run("sign", "--key-file", privateFile.toString(), "important message");
        assertThat(signed.exitCode()).isZero();
        String signature = signed.out().trim();
        assertThat(signature).isNotBlank();

        var verified = run("verify", "--key-file", publicFile.toString(), "--sig", signature, "important message");
        assertThat(verified.exitCode()).isZero();
        assertThat(verified.out().trim()).isEqualTo("true");

        // tampered input does not verify
        var tampered = run("verify", "--key-file", publicFile.toString(), "--sig", signature, "changed message");
        assertThat(tampered.exitCode()).isEqualTo(1);
        assertThat(tampered.out().trim()).isEqualTo("false");
    }

    @Test
    void signVerifyEd25519Roundtrip() throws Exception {
        var privateFile = tempDir.resolve("ed-private.pem");
        var publicFile = tempDir.resolve("ed-public.pem");
        run("create", "key", "-a", "ed25519", "--out-private", privateFile.toString(), "--out-public",
                publicFile.toString());

        var signed = run("sign", "--key-file", privateFile.toString(), "ed message");
        assertThat(signed.exitCode()).isZero();

        var verified = run("verify", "--key-file", publicFile.toString(), "--sig", signed.out().trim(), "ed message");
        assertThat(verified.exitCode()).isZero();

        // sig file variant
        var sigFile = tempDir.resolve("sig.b64");
        Files.writeString(sigFile, signed.out().trim());
        assertThat(run("verify", "--key-file", publicFile.toString(), "--sig-file", sigFile.toString(), "ed message")
                .exitCode()).isZero();
    }

    @Test
    void signVerifyEcRoundtrip() throws Exception {
        var privateFile = tempDir.resolve("ec-private.pem");
        var publicFile = tempDir.resolve("ec-public.pem");
        run("create", "key", "-a", "ec", "--curve", "secp384r1", "--out-private", privateFile.toString(),
                "--out-public", publicFile.toString());

        var signed = run("sign", "--key-file", privateFile.toString(), "ec message");
        assertThat(signed.exitCode()).isZero();

        assertThat(run("verify", "--key-file", publicFile.toString(), "--sig", signed.out().trim(), "ec message")
                .exitCode()).isZero();
    }

    @Test
    void signErrors() throws Exception {
        var privateFile = tempDir.resolve("p.pem");
        var publicFile = tempDir.resolve("pub.pem");
        run("create", "key", "--out-private", privateFile.toString(), "--out-public", publicFile.toString());

        // private key passed as public key -> error
        assertThat(run("verify", "--key-file", privateFile.toString(), "--sig", "AAAA", "x").exitCode()).isEqualTo(2);
        // missing signature
        assertThat(run("verify", "--key-file", publicFile.toString(), "x").exitCode()).isEqualTo(2);
        // invalid base64 signature
        assertThat(run("verify", "--key-file", publicFile.toString(), "--sig", "!!!", "x").exitCode()).isEqualTo(2);
    }

    @Test
    void createKeyPrintsPemToStdout() {
        var created = run("create", "key", "-a", "ed25519");
        assertThat(created.exitCode()).isZero();
        assertThat(created.out()).containsPattern("-----BEGIN PRIVATE KEY-----[\\s\\S]*-----END PRIVATE KEY-----");
        assertThat(created.out()).containsPattern("-----BEGIN PUBLIC KEY-----[\\s\\S]*-----END PUBLIC KEY-----");

        assertThat(run("create", "key", "-a", "nope").exitCode()).isEqualTo(2);
    }
}
