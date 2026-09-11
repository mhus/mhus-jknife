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
package de.mhus.jknife.jsec;

import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.security.MessageDigest;
import java.security.Security;
import java.util.concurrent.Callable;

/**
 * Hashes text or binary input with a MessageDigest algorithm.
 */
@Command(name = "hash", mixinStandardHelpOptions = true, description = "Hash the input with a MessageDigest algorithm (e.g. sha256, sha512, md5). Exit code 0 on success, 2 on error.")
public class HashCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", arity = "0..1", description = "The input to hash (default: read from stdin)")
    private String input;

    @Mixin
    private TextInput textInput;

    @Option(names = { "-a",
            "--alg" }, paramLabel = "NAME", defaultValue = "sha256", description = "MessageDigest algorithm (default: ${DEFAULT-VALUE}), e.g. sha1, sha256, sha384, sha512, md5")
    private String algorithm;

    @Option(names = { "-o",
            "--out" }, paramLabel = "FORMAT", defaultValue = "hex", description = "Output encoding: hex or base64 (default: ${DEFAULT-VALUE})")
    private String outputFormat;

    @Option(names = { "--list" }, description = "List the available MessageDigest algorithms")
    private boolean list;

    @Override
    public Integer call() throws Exception {
        if (list) {
            Security.getAlgorithms("MessageDigest").stream().sorted(String::compareToIgnoreCase)
                    .forEach(System.out::println);
            return 0;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm.toLowerCase());
        } catch (Exception e) {
            System.err.println("Unknown hash algorithm: " + algorithm + " (see --list)");
            return 2;
        }

        byte[] bytes = textInput.resolveBytes(input);
        if (bytes == null || bytes.length == 0) {
            System.err.println("Empty input");
            return 2;
        }

        byte[] hash = digest.digest(bytes);
        switch (outputFormat.toLowerCase()) {
        case "hex" -> System.out.println(SecUtil.toHex(hash));
        case "base64", "b64" -> System.out.println(SecUtil.toBase64(hash));
        default -> {
            System.err.println("Unknown output format: " + outputFormat + " (allowed: hex, base64)");
            return 2;
        }
        }
        return 0;
    }
}
