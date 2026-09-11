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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Creates a random symmetric secret (e.g. an aes key) and prints it as base64.
 */
@Command(name = "secret", mixinStandardHelpOptions = true, description = "Create a random symmetric secret (e.g. an aes key, usable for encrypt/decrypt --key) as base64. Exit code 0 on success, 2 on error.")
public class SecretCmd implements Callable<Integer> {

    @Option(names = { "-s",
            "--size" }, paramLabel = "BITS", defaultValue = "256", description = "Secret size in bits, must be a multiple of 8 (default: ${DEFAULT-VALUE}). 128/192/256 are valid aes sizes.")
    private int size;

    @Override
    public Integer call() {
        if (size <= 0 || size % 8 != 0) {
            System.err.println("Invalid secret size: " + size + " bits (must be a positive multiple of 8)");
            return 2;
        }
        System.out.println(SecUtil.toBase64(SecUtil.randomBytes(size / 8)));
        return 0;
    }
}
