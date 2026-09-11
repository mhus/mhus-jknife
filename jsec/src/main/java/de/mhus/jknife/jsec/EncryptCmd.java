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

import java.util.concurrent.Callable;

/**
 * Encrypts text or binary input with aes-gcm and prints the envelope as base64.
 */
@Command(name = "encrypt", mixinStandardHelpOptions = true, description = "Encrypt the input with aes-gcm and print the envelope as base64. Key from --key (base64), --key-file or derived from --password. Exit code 0 on success, 2 on error.")
public class EncryptCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", arity = "0..1", description = "The input to encrypt (default: read from stdin)")
    private String input;

    @Mixin
    private TextInput textInput;

    @Mixin
    private KeyOptions keyOptions;

    @Override
    public Integer call() throws Exception {
        byte[] plain = textInput.resolveBytes(input);
        if (plain == null || plain.length == 0) {
            System.err.println("Empty input");
            return 2;
        }

        try {
            byte[] keyBytes;
            byte[] salt = null;
            if (keyOptions.isPasswordMode()) {
                salt = SecUtil.randomBytes(SecUtil.SALT_BYTES);
                keyBytes = SecUtil.deriveKey(keyOptions.password(), salt, 256);
            } else {
                keyBytes = keyOptions.readKeyBytes();
                if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                    System.err.println("Invalid key size: " + keyBytes.length * 8
                            + " bits (aes needs 128, 192 or 256 bits, see 'jsec create secret')");
                    return 2;
                }
            }

            byte[] envelope = SecUtil.encryptAesGcm(keyBytes, plain, salt);
            System.out.println(SecUtil.toBase64(envelope));
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            return 2;
        }
    }
}
