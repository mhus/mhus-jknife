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
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/**
 * Decrypts a base64 aes-gcm envelope and writes the plain bytes to stdout.
 */
@Command(name = "decrypt", mixinStandardHelpOptions = true, description = "Decrypt a base64 aes-gcm envelope and write the plain output to stdout. Key source must match the encryption (see encrypt). Exit code 0 on success, 2 on error.")
public class DecryptCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ENVELOPE", arity = "0..1", description = "The base64 envelope to decrypt (default: read from stdin)")
    private String envelope;

    @Mixin
    private TextInput textInput;

    @Mixin
    private KeyOptions keyOptions;

    @Override
    public Integer call() throws Exception {
        String input = textInput.resolveText(envelope);
        if (input == null || input.isBlank()) {
            System.err.println("Empty input");
            return 2;
        }

        byte[] data;
        try {
            data = SecUtil.fromBase64(input.trim());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: not base64");
            return 2;
        }

        try {
            byte[] keyBytes;
            int saltLength;
            if (keyOptions.isPasswordMode()) {
                saltLength = SecUtil.SALT_BYTES;
                if (data.length < SecUtil.SALT_BYTES) {
                    System.err.println("Invalid envelope");
                    return 2;
                }
                byte[] salt = new byte[SecUtil.SALT_BYTES];
                System.arraycopy(data, 0, salt, 0, salt.length);
                keyBytes = SecUtil.deriveKey(keyOptions.password(), salt, 256);
            } else {
                saltLength = 0;
                keyBytes = keyOptions.readKeyBytes();
                if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                    System.err.println(
                            "Invalid key size: " + keyBytes.length * 8 + " bits (aes needs 128, 192 or 256 bits)");
                    return 2;
                }
            }

            byte[] plain = SecUtil.decryptAesGcm(keyBytes, data, saltLength);
            System.out.write(plain);
            System.out.flush();
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Decryption failed: wrong key or corrupted data");
            return 2;
        }
    }
}
