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

import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Common text/binary input options for jknife tools, usable as picocli mixin.
 */
public class TextInput {

    @Option(names = { "-t",
            "--text-file" }, paramLabel = "FILE", description = "Read the input from FILE or '-' for stdin (default: stdin if no TEXT argument is given)")
    private String textFile;

    @Option(names = { "-c",
            "--charset" }, description = "Charset of the text file, the TEXT argument or stdin (default: ${DEFAULT-VALUE})")
    private String charSet = "UTF-8";

    @Option(names = { "-v", "--verbose" }, description = "Print processing information to stderr")
    private boolean verbose;

    /**
     * Resolve the input text: either the TEXT argument, the text file / stdin.
     */
    public String resolveText(String text) throws IOException {
        if (text != null && textFile != null)
            throw new IllegalArgumentException("Use either a TEXT argument or --text-file, not both");
        if (textFile != null)
            return IOUtil.readText(textFile, charset());
        if (text != null)
            return text;
        return IOUtil.readText(IOUtil.STDIN, charset());
    }

    /**
     * Resolve the input as raw bytes: file/stdin bytes are taken as-is, a TEXT argument is converted using the charset
     * option.
     */
    public byte[] resolveBytes(String text) throws IOException {
        if (text != null && textFile != null)
            throw new IllegalArgumentException("Use either a TEXT argument or --text-file, not both");
        if (textFile != null)
            return IOUtil.readBytes(textFile);
        if (text != null)
            return text.getBytes(charset());
        return IOUtil.readBytes(IOUtil.STDIN);
    }

    public Charset charset() {
        return IOUtil.charset(charSet);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void verbose(String message) {
        if (verbose)
            System.err.println("[jknife] " + message);
    }
}
