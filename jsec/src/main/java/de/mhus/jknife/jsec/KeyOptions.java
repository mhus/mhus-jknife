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

import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Key source options for encrypt/decrypt (aes): one of --key (base64), --key-file or --password (pbkdf2 derived).
 */
public class KeyOptions {

    @Option(names = { "-k",
            "--key" }, paramLabel = "B64", description = "Symmetric key as base64 (see 'jsec create secret')")
    private String base64Key;

    @Option(names = { "--key-file" }, paramLabel = "FILE", description = "Key file: base64 or pem (SECRET KEY block)")
    private String keyFile;

    @Option(names = { "-p",
            "--password" }, paramLabel = "TEXT", description = "Derive the key from a password (PBKDF2WithHmacSHA256, random salt in the envelope)")
    private String password;

    public boolean isPasswordMode() {
        return password != null;
    }

    public char[] password() {
        return password.toCharArray();
    }

    /** reads the raw key bytes from --key or --key-file (not for password mode) */
    public byte[] readKeyBytes() throws IOException {
        if (base64Key != null && keyFile == null)
            return SecUtil.fromBase64(base64Key);
        if (keyFile != null && base64Key == null)
            return SecUtil.readPem(Files.readString(Path.of(keyFile)));
        throw new IllegalArgumentException(
                "Give exactly one key source: --key <base64>, --key-file <file> or --password <text>");
    }
}
