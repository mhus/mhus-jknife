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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.concurrent.Callable;

/**
 * Creates an asymmetric key pair and prints or writes it as pem (private pkcs#8, public x.509).
 */
@Command(name = "key", mixinStandardHelpOptions = true, description = "Create an asymmetric key pair (rsa, ec or ed25519) as pem: private key pkcs#8, public key x.509. Exit code 0 on success, 2 on error.")
public class KeyCmd implements Callable<Integer> {

    @Option(names = { "-a",
            "--algorithm" }, paramLabel = "NAME", defaultValue = "rsa", description = "Key algorithm: rsa (default), ec or ed25519")
    private String algorithm;

    @Option(names = { "-s",
            "--size" }, paramLabel = "BITS", defaultValue = "2048", description = "Rsa key size in bits (default: ${DEFAULT-VALUE})")
    private int size;

    @Option(names = {
            "--curve" }, paramLabel = "NAME", defaultValue = "secp256r1", description = "Ec curve name (default: ${DEFAULT-VALUE}), e.g. secp256r1, secp384r1, secp521r1")
    private String curve;

    @Option(names = {
            "--out-private" }, paramLabel = "FILE", description = "Write the private key pem to a file instead of stdout")
    private String outPrivate;

    @Option(names = {
            "--out-public" }, paramLabel = "FILE", description = "Write the public key pem to a file instead of stdout")
    private String outPublic;

    @Override
    public Integer call() throws Exception {
        KeyPair pair;
        try {
            switch (algorithm.toLowerCase()) {
            case "rsa" -> {
                var generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(size);
                pair = generator.generateKeyPair();
            }
            case "ec", "ecdsa" -> {
                var generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(new ECGenParameterSpec(curve));
                pair = generator.generateKeyPair();
            }
            case "ed25519", "eddsa" -> pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            default -> {
                System.err.println("Unknown key algorithm: " + algorithm + " (allowed: rsa, ec, ed25519)");
                return 2;
            }
            }
        } catch (Exception e) {
            System.err.println("Key generation failed: " + e.getMessage());
            return 2;
        }

        String privatePem = SecUtil.writePem("PRIVATE KEY", pair.getPrivate().getEncoded());
        String publicPem = SecUtil.writePem("PUBLIC KEY", pair.getPublic().getEncoded());

        if (outPrivate != null) {
            Files.writeString(Path.of(outPrivate), privatePem);
            System.out.println("private key written: " + outPrivate);
        }
        if (outPublic != null) {
            Files.writeString(Path.of(outPublic), publicPem);
            System.out.println("public key written: " + outPublic);
        }
        if (outPrivate == null)
            System.out.print(privatePem);
        if (outPublic == null)
            System.out.print(publicPem);
        return 0;
    }
}
