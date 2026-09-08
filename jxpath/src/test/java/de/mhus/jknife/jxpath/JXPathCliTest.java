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
package de.mhus.jknife.jxpath;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JXPathCliTest {

    private record Result(int exitCode, String out) {
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JXPathCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    private static final String BOOKS = "<books><book id=\"1\"><title>A</title></book>"
            + "<book id=\"2\"><title>B</title></book></books>";

    @Test
    void selectTextNodes() {
        var r = run("select", "//title", BOOKS);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).containsExactly("<title>A</title>", "<title>B</title>");
    }

    @Test
    void selectElementsAsXml() {
        var r = run("select", "//book", BOOKS);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("<book id=\"1\">");
        assertThat(r.out().lines().toList()).hasSize(2);
    }

    @Test
    void selectAttributes() {
        var r = run("select", "//book/@id", BOOKS);
        assertThat(r.out().lines().toList()).containsExactly("1", "2");
    }

    @Test
    void selectWithPredicate() {
        var r = run("select", "//book[@id='2']/title", BOOKS);
        assertThat(r.out().lines().toList()).containsExactly("<title>B</title>");
    }

    @Test
    void selectStringExpression() {
        var r = run("select", "count(//book)", BOOKS);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("2");
    }

    @Test
    void selectNamespacedXml() {
        var xml = "<root xmlns:x=\"http://example.com/ns\"><x:item>v1</x:item></root>";
        var withNs = run("select", "--ns", "x=http://example.com/ns", "//x:item", xml);
        assertThat(withNs.exitCode()).isZero();
        assertThat(withNs.out().trim()).contains("<x:item").contains(">v1</x:item>");

        // without namespace mapping: no match
        var withoutNs = run("select", "//x:item", xml);
        assertThat(withoutNs.exitCode()).isEqualTo(1);
        assertThat(withoutNs.out()).isEmpty();
    }

    @Test
    void selectInvalidXPath() {
        assertThat(run("select", "//[", BOOKS).exitCode()).isEqualTo(2);
    }

    @Test
    void selectInvalidXml() {
        assertThat(run("select", "//a", "not xml").exitCode()).isEqualTo(2);
    }

    @Test
    void exists() {
        assertThat(run("exists", "//book", BOOKS).exitCode()).isZero();
        assertThat(run("exists", "//book", BOOKS).out().trim()).isEqualTo("true");
        assertThat(run("exists", "//cd", BOOKS).exitCode()).isEqualTo(1);
        assertThat(run("exists", "//cd", BOOKS).out().trim()).isEqualTo("false");
    }

    @Test
    void existsQuiet() {
        var r = run("exists", "-q", "//book", BOOKS);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).isEmpty();
    }

    @Test
    void rejectDoctype() {
        assertThat(run("select", "//a", "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><a/>").exitCode())
                .isEqualTo(2);
    }
}
