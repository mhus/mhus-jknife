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

import java.util.concurrent.Callable;

/**
 * jsec - security tool family based on the java security framework.
 *
 * Subcommands: hash, create (key, secret), encrypt, decrypt, sign, verify.
 */
@Command(name = "jsec", mixinStandardHelpOptions = true, version = "jsec "
        + JsecCmd.VERSION, description = "Security tool family based on the java security framework (hash, keys, encrypt, sign).", subcommands = {
                CommandLine.HelpCommand.class, HashCmd.class, CreateCmd.class, EncryptCmd.class, DecryptCmd.class,
                SignCmd.class, VerifyCmd.class })
public class JsecCmd implements Callable<Integer> {

    /** keep in sync with the maven project version */
    public static final String VERSION = "0.3.0";

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new JsecCmd()).execute(args);
        System.exit(exitCode);
    }
}
