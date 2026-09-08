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
package de.mhus.jknife.jjson;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JJsonCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JJsonCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void validateValid() {
        assertThat(run("validate", "{\"a\":1}").exitCode()).isZero();
        assertThat(run("validate", "[1,2,3]").exitCode()).isZero();
        assertThat(run("validate", "\"text\"").exitCode()).isZero();
        assertThat(run("validate", "null").exitCode()).isZero();
    }

    @Test
    void validateInvalid() {
        assertThat(run("validate", "{a:1}").exitCode()).isEqualTo(2);
        assertThat(run("validate", "").exitCode()).isEqualTo(2);
    }

    @Test
    void prettyPrint() {
        var r = run("pretty", "{\"a\":1,\"b\":[1,2]}");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("\"a\" : 1");
        assertThat(r.out()).contains("\"b\" : [");
        assertThat(r.out()).contains("  \"a\" : 1");
        // roundtrip: pretty output is still valid json
        assertThat(run("validate", r.out().trim()).exitCode()).isZero();
    }

    @Test
    void prettyIndent() {
        var r = run("pretty", "-i", "4", "{\"a\":1}");
        assertThat(r.out()).contains("    \"a\" : 1");
    }

    @Test
    void compact() {
        var pretty = "{\n  \"a\" : 1,\n  \"b\" : [ 1, 2 ]\n}";
        var r = run("compact", pretty);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("{\"a\":1,\"b\":[1,2]}");
    }

    @Test
    void getSimple() {
        assertThat(run("get", "a", "{\"a\":\"hello\"}").out().trim()).isEqualTo("hello");
        assertThat(run("get", "a", "{\"a\":42}").out().trim()).isEqualTo("42");
        assertThat(run("get", "a", "{\"a\":true}").out().trim()).isEqualTo("true");
        assertThat(run("get", "a", "{\"a\":null}").out().trim()).isEqualTo("null");
    }

    @Test
    void getNested() {
        var json = "{\"store\":{\"book\":[{\"title\":\"A\",\"price\":9.9},{\"title\":\"B\"}]}}";
        assertThat(run("get", "store.book[0].title", json).out().trim()).isEqualTo("A");
        assertThat(run("get", "store.book[1].title", json).out().trim()).isEqualTo("B");
        assertThat(run("get", "store.book[0].price", json).out().trim()).isEqualTo("9.9");
        assertThat(run("get", "store.book[0]", json).out().trim()).isEqualTo("{\"title\":\"A\",\"price\":9.9}");
        assertThat(run("get", "store", json).out().trim()).startsWith("{\"book\":");
    }

    @Test
    void getWithDollarPrefix() {
        assertThat(run("get", "$.a.b", "{\"a\":{\"b\":\"x\"}}").out().trim()).isEqualTo("x");
        assertThat(run("get", "$", "[1,2]").out().trim()).isEqualTo("[1,2]");
    }

    @Test
    void getRootArray() {
        assertThat(run("get", "[0]", "[\"x\",\"y\"]").out().trim()).isEqualTo("x");
        assertThat(run("get", "[1]", "[\"x\",\"y\"]").out().trim()).isEqualTo("y");
    }

    @Test
    void getNotFound() {
        assertThat(run("get", "x", "{\"a\":1}").exitCode()).isEqualTo(1);
        assertThat(run("get", "a.b", "{\"a\":1}").exitCode()).isEqualTo(1);
    }

    @Test
    void getInvalidPathAndJson() {
        assertThat(run("get", "a[b", "{\"a\":1}").exitCode()).isEqualTo(2);
        assertThat(run("get", "a", "not json").exitCode()).isEqualTo(2);
    }

    @Test
    void getPrettyContainer() {
        var r = run("get", "-p", "a", "{\"a\":{\"b\":1}}");
        assertThat(r.out()).contains("\"b\" : 1");
    }
}
