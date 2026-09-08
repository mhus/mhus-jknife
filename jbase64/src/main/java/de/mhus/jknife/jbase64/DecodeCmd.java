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
package de.mhus.jknife.jbase64;

import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Base64;
import java.util.concurrent.Callable;

/**
 * Decodes base64 text (argument, file or stdin) and writes the raw result to stdout.
 */
@Command(name = "decode", mixinStandardHelpOptions = true, description = "Decode base64 text and write the raw result to stdout. Whitespace in the input is ignored. Exit code 0 on success, 2 on error.")
public class DecodeCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "TEXT", arity = "0..1", description = "The base64 text to decode (default: read from stdin)")
    private String text;

    @Mixin
    private TextInput input;

    @Option(names = { "-u",
            "--urlsafe" }, description = "Use the URL and filename safe alphabet ('-' and '_' instead of '+' and '/')")
    private boolean urlsafe;

    @Override
    public Integer call() throws Exception {
        String base64 = input.resolveText(text).replaceAll("\\s+", "");
        input.verbose("Input: " + base64);

        var decoder = urlsafe ? Base64.getUrlDecoder() : Base64.getDecoder();
        try {
            byte[] decoded = decoder.decode(base64);
            System.out.write(decoded);
            System.out.flush();
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid base64 input: " + e.getMessage());
            return 2;
        }
    }
}
