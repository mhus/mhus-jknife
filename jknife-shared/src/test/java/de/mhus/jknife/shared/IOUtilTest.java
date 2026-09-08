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
package de.mhus.jknife.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IOUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void readTextFromFile() throws IOException {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        assertThat(IOUtil.readText(file.toString(), null)).isEqualTo("hello world");
    }

    @Test
    void readTextFromStdinMarker() throws IOException {
        var oldIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("from stdin".getBytes(StandardCharsets.UTF_8)));
            assertThat(IOUtil.readText(IOUtil.STDIN, null)).isEqualTo("from stdin");
            System.setIn(new ByteArrayInputStream("from stdin".getBytes(StandardCharsets.UTF_8)));
            assertThat(IOUtil.readText(null, null)).isEqualTo("from stdin");
        } finally {
            System.setIn(oldIn);
        }
    }

    @Test
    void charsetFallback() {
        assertThat(IOUtil.charset((Charset) null)).isEqualTo(StandardCharsets.UTF_8);
        assertThat(IOUtil.charset((String) null)).isEqualTo(StandardCharsets.UTF_8);
        assertThat(IOUtil.charset("ISO-8859-1")).isEqualTo(StandardCharsets.ISO_8859_1);
    }
}
