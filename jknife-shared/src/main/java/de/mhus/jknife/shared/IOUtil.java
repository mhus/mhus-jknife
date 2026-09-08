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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small IO helpers shared by all jknife tools.
 */
public final class IOUtil {

    public static final String STDIN = "-";
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private IOUtil() {
    }

    /**
     * Read text from a file or from stdin (if file is null, empty or "-").
     *
     * @param fileOrStdin
     *            file path or "-" for stdin
     * @param charset
     *            charset to use, null means UTF-8
     */
    public static String readText(String fileOrStdin, Charset charset) throws IOException {
        if (fileOrStdin == null || fileOrStdin.isBlank() || STDIN.equals(fileOrStdin))
            return readAll(System.in, charset);
        return Files.readString(Path.of(fileOrStdin), charset(charset));
    }

    public static String readAll(InputStream in, Charset charset) throws IOException {
        return new String(in.readAllBytes(), charset(charset));
    }

    public static Charset charset(Charset charset) {
        return charset == null ? DEFAULT_CHARSET : charset;
    }

    public static Charset charset(String name) {
        return name == null || name.isBlank() ? DEFAULT_CHARSET : Charset.forName(name);
    }
}
