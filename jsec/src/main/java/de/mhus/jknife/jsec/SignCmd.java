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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.concurrent.Callable;

/**
 * Signs the input with a private key (pem pkcs#8) and prints the signature as base64.
 */
@Command(name = "sign", mixinStandardHelpOptions = true, description = "Sign the input with a private key (pem, pkcs#8) and print the signature as base64. Algorithm defaults by key type: rsa -> SHA256withRSA, ec -> SHA256withECDSA, ed25519 -> Ed25519. Exit code 0 on success, 2 on error.")
public class SignCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", arity = "0..1", description = "The input to sign (default: read from stdin)")
    private String input;

    @Mixin
    private TextInput textInput;

    @Option(names = {
            "--key-file" }, paramLabel = "FILE", required = true, description = "Private key pem file (pkcs#8, see 'jsec create key')")
    private String keyFile;

    @Option(names = { "-a",
            "--alg" }, paramLabel = "NAME", description = "Signature algorithm (default: by key type, e.g. SHA256withRSA)")
    private String algorithm;

    @Override
    public Integer call() throws Exception {
        byte[] data = textInput.resolveBytes(input);
        if (data == null || data.length == 0) {
            System.err.println("Empty input");
            return 2;
        }

        try {
            PrivateKey key = SecUtil.readPrivateKey(SecUtil.readPem(Files.readString(Path.of(keyFile))));
            String algorithm = this.algorithm != null ? this.algorithm : SecUtil.defaultSignatureAlgorithm(key);

            var signature = Signature.getInstance(algorithm);
            signature.initSign(key);
            signature.update(data);
            System.out.println(SecUtil.toBase64(signature.sign()));
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Signing failed: " + e.getMessage());
            return 2;
        }
    }
}
