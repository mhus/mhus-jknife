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
package de.mhus.jknife.juuid;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JUuidCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JUuidCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Test
    void generateDefaultV4() {
        var r = run("generate");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).hasSize(1);
        assertThat(r.out().trim()).matches(UUID_PATTERN);
        assertThat(r.out().trim()).containsPattern("-4");
    }

    @Test
    void generateCount() {
        var r = run("generate", "-n", "5");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).hasSize(5);
    }

    @Test
    void generateUpper() {
        var r = run("generate", "--upper");
        assertThat(r.out().trim()).matches("^[0-9A-F-]{36}$");
    }

    @Test
    void generateV7Sortable() {
        var r = run("generate", "-t", "7", "-n", "3");
        assertThat(r.exitCode()).isZero();
        var lines = r.out().lines().toList();
        assertThat(lines).hasSize(3);
        lines.forEach(line -> assertThat(UUID.fromString(line).version()).isEqualTo(7));
        // v7 uuids generated in order are sortable (monotonic timestamp prefix)
        assertThat(lines.get(0)).isLessThanOrEqualTo(lines.get(1));
        assertThat(lines.get(1)).isLessThanOrEqualTo(lines.get(2));
    }

    @Test
    void parseV7ShowsTimestamp() {
        var uuid = UuidUtil.generateV7();
        var r = run("parse", uuid.toString());
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("version: 7");
        assertThat(r.out()).contains("variant: 2 (RFC 4122 / RFC 9562)");
        assertThat(r.out()).contains("timestamp: "
                + Instant.ofEpochMilli(UuidUtil.v7Timestamp(uuid).toEpochMilli()).toString().replace("Z", "Z"));
    }

    @Test
    void parseV4() {
        var uuid = UUID.randomUUID();
        var r = run("parse", uuid.toString());
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("version: 4");
        assertThat(r.out()).doesNotContain("timestamp");
    }

    @Test
    void parseUpperCaseInput() {
        var r = run("parse", "6FAB0F06-4B61-4E8B-8C1D-9F0A0A0A0A0A");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("value: 6fab0f06-4b61-4e8b-8c1d-9f0a0a0a0a0a");
    }

    @Test
    void parseInvalid() {
        assertThat(run("parse", "not-a-uuid").exitCode()).isEqualTo(2);
    }

    @Test
    void generateInvalidType() {
        assertThat(run("generate", "-t", "5").exitCode()).isEqualTo(2);
        assertThat(run("generate", "-n", "0").exitCode()).isEqualTo(2);
    }
}
