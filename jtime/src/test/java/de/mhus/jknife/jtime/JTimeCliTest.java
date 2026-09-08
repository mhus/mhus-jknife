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
package de.mhus.jknife.jtime;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class JTimeCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JTimeCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    private static final long SECONDS = 1725787000L;
    private static final long MILLIS = 1725787000123L;
    private static final Instant SECONDS_INSTANT = Instant.ofEpochSecond(SECONDS);
    private static final Instant MILLIS_INSTANT = Instant.ofEpochMilli(MILLIS);

    @Test
    void parseEpochSeconds() {
        var r = run("parse", String.valueOf(SECONDS));
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("epoch: " + SECONDS);
        assertThat(r.out()).contains("epoch-millis: " + SECONDS * 1000);
        assertThat(r.out()).contains("iso-utc: " + SECONDS_INSTANT);
    }

    @Test
    void parseEpochMillis() {
        var r = run("parse", String.valueOf(MILLIS));
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("epoch: " + MILLIS / 1000);
        assertThat(r.out()).contains("epoch-millis: " + MILLIS);
        assertThat(r.out()).contains("iso-utc: " + MILLIS_INSTANT);
    }

    @Test
    void parseIsoInstant() {
        var r = run("parse", SECONDS_INSTANT.toString());
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("epoch: " + SECONDS);
    }

    @Test
    void parseIsoOffset() {
        // same instant, expressed with +02:00 offset
        var r = run("parse", SECONDS_INSTANT.atOffset(java.time.ZoneOffset.ofHours(2)).toString());
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("epoch: " + SECONDS);
    }

    @Test
    void parseLocalDateTimeUsesZone() {
        // 2024-09-08T12:00:00 in UTC = 1725796800
        var r = run("parse", "-z", "UTC", "2024-09-08T12:00:00");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("epoch: 1725796800");
    }

    @Test
    void parseWithZoneOutput() {
        var r = run("parse", "-z", "Europe/Berlin", String.valueOf(SECONDS));
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso[Europe/Berlin]: 2024-09-08T");
        assertThat(r.out()).contains("+02:00");
    }

    @Test
    void parseCustomFormat() {
        var r = run("parse", "-f", "yyyy-MM-dd", String.valueOf(SECONDS));
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).containsExactly("2024-09-08");
    }

    @Test
    void parseInvalid() {
        assertThat(run("parse", "not-a-timestamp").exitCode()).isEqualTo(2);
        assertThat(run("parse", "1234567890123456").exitCode()).isEqualTo(2); // 16 digits
        assertThat(run("parse", "-z", "Not/AZone", "1725787000").exitCode()).isEqualTo(2);
    }

    @Test
    void nowDefault() {
        var r = run("now");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    }

    @Test
    void nowEpoch() {
        var r = run("now", "--epoch");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).matches("\\d{10}");
    }

    @Test
    void nowMillis() {
        var r = run("now", "--millis");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).matches("\\d{13}");
    }

    @Test
    void nowCustomFormatAndZone() {
        var r = run("now", "-z", "UTC", "-f", "HH:mm");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).matches("\\d{2}:\\d{2}");
    }

    @Test
    void nowConflictingOptions() {
        assertThat(run("now", "--epoch", "--millis").exitCode()).isEqualTo(2);
    }
}
