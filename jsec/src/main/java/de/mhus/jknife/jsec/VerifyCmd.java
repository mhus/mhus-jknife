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
import java.security.PublicKey;
import java.security.Signature;
import java.util.concurrent.Callable;

/**
 * Verifies a signature with a public key (pem x.509). Exit code 0 if valid, 1 if not, 2 on error.
 */
@Command(name = "verify", mixinStandardHelpOptions = true, description = "Verify a signature with a public key (pem, x.509). Exit code 0 if valid, 1 if not, 2 on error.")
public class VerifyCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", arity = "0..1", description = "The input that was signed (default: read from stdin)")
    private String input;

    @Mixin
    private TextInput textInput;

    @Option(names = {
            "--key-file" }, paramLabel = "FILE", required = true, description = "Public key pem file (x.509, see 'jsec create key')")
    private String keyFile;

    @Option(names = { "--sig" }, paramLabel = "B64", description = "The signature as base64")
    private String signatureBase64;

    @Option(names = { "--sig-file" }, paramLabel = "FILE", description = "Signature file (base64)")
    private String signatureFile;

    @Option(names = { "-a", "--alg" }, paramLabel = "NAME", description = "Signature algorithm (default: by key type)")
    private String algorithm;

    @Override
    public Integer call() throws Exception {
        byte[] data = textInput.resolveBytes(input);
        if (data == null || data.length == 0) {
            System.err.println("Empty input");
            return 2;
        }

        String signatureText = signatureBase64 != null ? signatureBase64.trim() : null;
        if (signatureText == null && signatureFile != null)
            signatureText = Files.readString(Path.of(signatureFile)).trim();
        if (signatureText == null || signatureText.isBlank()) {
            System.err.println("No signature given: use --sig <base64> or --sig-file <file>");
            return 2;
        }

        try {
            PublicKey key = SecUtil.readPublicKey(SecUtil.readPem(Files.readString(Path.of(keyFile))));
            String algorithm = this.algorithm != null ? this.algorithm : SecUtil.defaultSignatureAlgorithm(key);

            var signature = Signature.getInstance(algorithm);
            signature.initVerify(key);
            signature.update(data);
            boolean valid = signature.verify(SecUtil.fromBase64(signatureText));
            System.out.println(valid);
            return valid ? 0 : 1;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Verification failed: " + e.getMessage());
            return 2;
        }
    }
}
