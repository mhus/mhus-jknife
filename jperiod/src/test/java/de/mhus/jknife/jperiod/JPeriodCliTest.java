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
package de.mhus.jknife.jperiod;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JPeriodCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JPeriodCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void parseShorthand() {
        var r = run("parse", "2h30m");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso: PT2H30M");
        assertThat(r.out()).contains("human: 2h 30m");
        assertThat(r.out()).contains("seconds: 9000");
        assertThat(r.out()).contains("millis: 9000000");
        assertThat(r.out()).contains("minutes: 150");
        assertThat(r.out()).contains("hours: 2.5");
    }

    @Test
    void parseWithUnit() {
        assertThat(run("parse", "-u", "m", "2h30m").out().trim()).isEqualTo("150");
        assertThat(run("parse", "-u", "h", "90m").out().trim()).isEqualTo("1.5");
        assertThat(run("parse", "-u", "minutes", "2h30m").out().trim()).isEqualTo("150");
        assertThat(run("parse", "-u", "s", "90").out().trim()).isEqualTo("90");
        assertThat(run("parse", "-u", "d", "1w").out().trim()).isEqualTo("7");
    }

    @Test
    void parseIso() {
        var r = run("parse", "PT90S");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso: PT1M30S");
        assertThat(r.out()).contains("human: 1m 30s");

        var days = run("parse", "P2DT3H");
        assertThat(days.out()).contains("human: 2d 3h");
        assertThat(days.out()).contains("hours: 51");

        var weeks = run("parse", "P1W");
        assertThat(weeks.out()).contains("days: 7");
    }

    @Test
    void parseBareNumberIsSeconds() {
        var r = run("parse", "90");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso: PT1M30S");
    }

    @Test
    void parseDecimalAndComma() {
        assertThat(run("parse", "-u", "s", "1.5h").out().trim()).isEqualTo("5400");
        assertThat(run("parse", "-u", "s", "1,5h").out().trim()).isEqualTo("5400");
        assertThat(run("parse", "-u", "s", "500ms").out().trim()).isEqualTo("0.5");
    }

    @Test
    void parseFractionalSecondsHuman() {
        assertThat(run("parse", "1m30.5s").out()).contains("human: 1m 30.5s");
        assertThat(run("parse", "45s").out()).contains("human: 45s");
    }

    @Test
    void parseNegative() {
        var r = run("parse", "--", "-90s");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso: PT-1M-30S");
        assertThat(r.out()).contains("human: -1m 30s");
        assertThat(r.out()).contains("seconds: -90");
    }

    @Test
    void parseZero() {
        var r = run("parse", "0");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("human: 0s");
        assertThat(r.out()).contains("iso: PT0S");
    }

    @Test
    void parseInvalid() {
        assertThat(run("parse", "abc").exitCode()).isEqualTo(2);
        assertThat(run("parse", "5x").exitCode()).isEqualTo(2);
        assertThat(run("parse", "1h30").exitCode()).isEqualTo(2); // missing unit
        assertThat(run("parse", "P1Y").exitCode()).isEqualTo(2); // years rejected
        assertThat(run("parse", "P1M").exitCode()).isEqualTo(2); // months rejected
        assertThat(run("parse", "-u", "years", "1h").exitCode()).isEqualTo(2); // unknown unit
    }

    @Test
    void addDurations() {
        var r = run("add", "2h30m", "45m");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso: PT3H15M");
        assertThat(r.out()).contains("minutes: 195");

        var sum = run("add", "-u", "h", "2h30m", "45m", "1h");
        assertThat(sum.out().trim()).isEqualTo("4.25");

        var iso = run("add", "P1D", "PT12H");
        assertThat(iso.out()).contains("hours: 36");
    }

    @Test
    void subDurations() {
        var r = run("sub", "5h", "90m");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("iso: PT3H30M");
        assertThat(r.out()).contains("minutes: 210");

        var negative = run("sub", "90m", "2h");
        assertThat(negative.out()).contains("minutes: -30");
    }

    @Test
    void addInvalid() {
        assertThat(run("add", "2h", "xyz").exitCode()).isEqualTo(2);
        assertThat(run("sub", "2h", "xyz").exitCode()).isEqualTo(2);
    }
}
