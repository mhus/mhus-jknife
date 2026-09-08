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
package de.mhus.jknife.jbase64;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JBase64CliTest {

    private record Result(int exitCode, String out, byte[] raw) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JBase64Cmd()).execute(args);
            var raw = buffer.toByteArray();
            return new Result(exit, new String(raw, StandardCharsets.UTF_8), raw);
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void encodeSimple() {
        var r = run("encode", "hello");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("aGVsbG8=");
    }

    @Test
    void encodeUtf8AndRoundtrip() {
        var text = "hällö wörld üüü";
        var encoded = run("encode", text);
        assertThat(encoded.exitCode()).isZero();

        var decoded = run("decode", encoded.out().trim());
        assertThat(decoded.exitCode()).isZero();
        assertThat(new String(decoded.raw(), StandardCharsets.UTF_8)).isEqualTo(text);
    }

    @Test
    void encodeUrlsafe() {
        // bytes that produce '+' and '/' in standard base64
        var bytes = new byte[] { (byte) 0xfb, (byte) 0xff, (byte) 0xbf };
        var expected = Base64.getUrlEncoder().encodeToString(bytes);

        var encoded = run("encode", "-u", "--text-file", writeTemp(bytes).toString());
        assertThat(encoded.out().trim()).isEqualTo(expected);
        assertThat(encoded.out()).doesNotContain("+").doesNotContain("/");
    }

    @Test
    void decodeSimple() {
        var r = run("decode", "aGVsbG8=");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).isEqualTo("hello");
    }

    @Test
    void decodeBinaryBytes() {
        var bytes = new byte[] { 0x00, 0x01, 0x0a, (byte) 0xff, 0x41 };
        var b64 = Base64.getEncoder().encodeToString(bytes);

        var r = run("decode", b64);
        assertThat(r.exitCode()).isZero();
        assertThat(r.raw()).containsExactly(bytes);
    }

    @Test
    void decodeIgnoresWhitespace() {
        var r = run("decode", "aGVs\nbG8=  \n");
        assertThat(r.out()).isEqualTo("hello");
    }

    @Test
    void decodeWrappedRoundtrip() {
        // "test" -> dGVzdA== (8 chars), wrapped at 4
        var encoded = run("encode", "-w", "4", "test");
        assertThat(encoded.out().lines().toList()).containsExactly("dGVz", "dA==");

        var decoded = run("decode", encoded.out().trim());
        assertThat(new String(decoded.raw(), StandardCharsets.UTF_8)).isEqualTo("test");
    }

    @Test
    void encodeFromStdin() {
        var oldIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("stdin text".getBytes(StandardCharsets.UTF_8)));
            var r = run("encode");
            assertThat(r.exitCode()).isZero();
            assertThat(r.out().trim())
                    .isEqualTo(Base64.getEncoder().encodeToString("stdin text".getBytes(StandardCharsets.UTF_8)));
        } finally {
            System.setIn(oldIn);
        }
    }

    @Test
    void invalidBase64() {
        assertThat(run("decode", "!!!not-base64!!!").exitCode()).isEqualTo(2);
    }

    @Test
    void textFileOption() throws Exception {
        var file = java.nio.file.Files.createTempFile("jbase64", ".txt");
        java.nio.file.Files.writeString(file, "file content");
        assertThat(run("encode", "-t", file.toString()).out().trim())
                .isEqualTo(Base64.getEncoder().encodeToString("file content".getBytes(StandardCharsets.UTF_8)));
    }

    private static java.nio.file.Path writeTemp(byte[] bytes) {
        try {
            var file = java.nio.file.Files.createTempFile("jbase64", ".bin");
            java.nio.file.Files.write(file, bytes);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
