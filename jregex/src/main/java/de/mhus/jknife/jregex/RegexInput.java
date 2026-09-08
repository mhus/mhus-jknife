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

import de.mhus.jknife.shared.IOUtil;
import picocli.CommandLine.Option;

import java.io.IOException;

/**
 * Common options for all jregex subcommands.
 */
public class RegexInput {

    @Option(names = { "-t",
            "--text-file" }, paramLabel = "FILE", description = "Read the text from FILE or '-' for stdin (default: stdin if no TEXT argument is given)")
    private String textFile;

    @Option(names = { "-c",
            "--charset" }, description = "Charset of the text file or stdin (default: ${DEFAULT-VALUE})")
    private String charSet = "UTF-8";

    @Option(names = { "-f", "--flags" }, description = "Regex flags, comma separated: " + RegexFlags.FLAG_NAMES)
    private String flagsStr = "";

    @Option(names = { "-v", "--verbose" }, description = "Print processing information to stderr")
    private boolean verbose;

    /**
     * Resolve the input text: either the TEXT argument, the text file / stdin.
     */
    public String resolveText(String text) throws IOException {
        if (text != null && textFile != null)
            throw new IllegalArgumentException("Use either a TEXT argument or --text-file, not both");
        if (textFile != null)
            return IOUtil.readText(textFile, IOUtil.charset(charSet));
        if (text != null)
            return text;
        return IOUtil.readText(IOUtil.STDIN, IOUtil.charset(charSet));
    }

    public int flags() {
        return RegexFlags.parse(flagsStr);
    }

    public String flagsStr() {
        return flagsStr;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void verbose(String message) {
        if (verbose)
            System.err.println("[jregex] " + message);
    }
}
