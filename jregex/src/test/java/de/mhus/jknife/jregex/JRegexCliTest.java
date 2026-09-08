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
package de.mhus.jknife.jregex;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JRegexCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JRegexCmd()).execute(args);
            return new Result(exit, buffer.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void matchFull() {
        var r = run("match", "a+c", "aaac");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("true");

        assertThat(run("match", "a+c", "bbb").exitCode()).isEqualTo(1);
        assertThat(run("match", "a+c", "bbb").out()).contains("false");
    }

    @Test
    void matchQuiet() {
        var r = run("match", "-q", "^\\d+$", "12345");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).isEmpty();
    }

    @Test
    void matchFlags() {
        assertThat(run("match", "-f", "CASE_INSENSITIVE", "^hello$", "HELLO").exitCode()).isZero();
        assertThat(run("match", "^hello$", "HELLO").exitCode()).isEqualTo(1);
    }

    @Test
    void invalidRegex() {
        assertThat(run("match", "[", "text").exitCode()).isEqualTo(2);
    }

    @Test
    void findMatches() {
        var r = run("find", "\\d+", "a 12 b 345 c");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).containsExactly("12", "345");
    }

    @Test
    void findNoMatch() {
        var r = run("find", "zzz", "abc");
        assertThat(r.exitCode()).isEqualTo(1);
        assertThat(r.out()).isEmpty();
    }

    @Test
    void findGroup() {
        var r = run("find", "-g", "1", "(\\w+)@(\\w+)", "max@foo anna@bar");
        assertThat(r.out().lines().toList()).containsExactly("max", "anna");
    }

    @Test
    void findCount() {
        var r = run("find", "--count", "a", "banana");
        assertThat(r.out().trim()).isEqualTo("3");
    }

    @Test
    void replaceAll() {
        var r = run("replace", "(\\w+)@(\\w+)\\.de", "$2/$1", "mike@web.de x");
        assertThat(r.out().trim()).isEqualTo("web/mike x");
    }

    @Test
    void replaceFirstOnly() {
        var r = run("replace", "--first", "a", "b", "aaa");
        assertThat(r.out().trim()).isEqualTo("baa");
    }

    @Test
    void replaceRespectsFlags() {
        var r = run("replace", "-f", "CASE_INSENSITIVE", "hello", "bye", "HeLLo hello");
        assertThat(r.out().trim()).isEqualTo("bye bye");
    }
}
