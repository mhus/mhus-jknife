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
 * Encodes text or binary input (argument, file or stdin) to base64.
 */
@Command(name = "encode", mixinStandardHelpOptions = true, description = "Encode text or binary input to base64 and print it. Exit code 0 on success, 2 on error.")
public class EncodeCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "TEXT", arity = "0..1", description = "The text to encode (default: read from stdin)")
    private String text;

    @Mixin
    private TextInput input;

    @Option(names = { "-u",
            "--urlsafe" }, description = "Use the URL and filename safe alphabet ('-' and '_' instead of '+' and '/')")
    private boolean urlsafe;

    @Option(names = { "-w",
            "--wrap" }, paramLabel = "N", defaultValue = "0", description = "Wrap output lines after N characters (0 = no wrapping)")
    private int wrap;

    @Override
    public Integer call() throws Exception {
        byte[] bytes = input.resolveBytes(text);
        input.verbose("Input: " + bytes.length + " bytes");

        var encoder = urlsafe ? Base64.getUrlEncoder() : Base64.getEncoder();
        String encoded = encoder.encodeToString(bytes);
        if (wrap > 0)
            encoded = wrap(encoded, wrap);
        System.out.println(encoded);
        return 0;
    }

    private static String wrap(String s, int n) {
        if (n <= 0 || s.length() <= n)
            return s;
        var sb = new StringBuilder();
        for (int i = 0; i < s.length(); i += n) {
            if (i > 0)
                sb.append('\n');
            sb.append(s, i, Math.min(s.length(), i + n));
        }
        return sb.toString();
    }
}
