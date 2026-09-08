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
package de.mhus.jknife.jyaml;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JYamlCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JYamlCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void validateValid() {
        assertThat(run("validate", "a: 1").exitCode()).isZero();
        assertThat(run("validate", "--", "- a\n- b\n").exitCode()).isZero();
        assertThat(run("validate", "just a scalar").exitCode()).isZero();
    }

    @Test
    void validateInvalid() {
        assertThat(run("validate", "a:\n  - b\n c: 1").exitCode()).isEqualTo(2);
    }

    @Test
    void tojsonSimple() {
        var r = run("tojson", "a: 1\nb: text");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("{\"a\":1,\"b\":\"text\"}");
    }

    @Test
    void tojsonNested() {
        var yaml = "server:\n  port: 8080\n  hosts:\n    - one\n    - two\nenabled: true\nratio: 0.5";
        var r = run("tojson", yaml);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim())
                .isEqualTo("{\"server\":{\"port\":8080,\"hosts\":[\"one\",\"two\"]},\"enabled\":true,\"ratio\":0.5}");
    }

    @Test
    void tojsonList() {
        var r = run("tojson", "--", "- 1\n- two\n- true");
        assertThat(r.out().trim()).isEqualTo("[1,\"two\",true]");
    }

    @Test
    void tojsonEmptyAndNull() {
        assertThat(run("tojson", "").out().trim()).isEqualTo("null");
        assertThat(run("tojson", "~").out().trim()).isEqualTo("null");
    }

    @Test
    void tojsonPretty() {
        var r = run("tojson", "-p", "a: 1");
        assertThat(r.out()).contains("\"a\" : 1");
    }

    @Test
    void tojsonNonStringKeys() {
        var r = run("tojson", "1: one");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("{\"1\":\"one\"}");
    }

    @Test
    void tojsonInvalid() {
        assertThat(run("tojson", "a: [1, 2").exitCode()).isEqualTo(2);
    }
}
