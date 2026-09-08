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

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * jbase64 - base64 encode/decode helper tool.
 *
 * Subcommands: encode, decode.
 */
@Command(name = "jbase64", mixinStandardHelpOptions = true, version = "jbase64 "
        + JBase64Cmd.VERSION, description = "Base64 encode/decode helper tool.", subcommands = {
                CommandLine.HelpCommand.class, EncodeCmd.class, DecodeCmd.class })
public class JBase64Cmd implements Callable<Integer> {

    /** keep in sync with the maven project version */
    public static final String VERSION = "0.2.0";

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new JBase64Cmd()).execute(args);
        System.exit(exitCode);
    }
}
