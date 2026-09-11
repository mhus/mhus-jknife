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
package de.mhus.jknife.jjson;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * jjson - JSON helper tool.
 *
 * Subcommands: pretty, compact, validate, get.
 */
@Command(name = "jjson", mixinStandardHelpOptions = true, version = "jjson "
        + JJsonCmd.VERSION, description = "JSON helper tool (validate, pretty, compact, get).", subcommands = {
                CommandLine.HelpCommand.class, PrettyCmd.class, CompactCmd.class, ValidateCmd.class, GetCmd.class })
public class JJsonCmd implements Callable<Integer> {

    /** keep in sync with the maven project version */
    public static final String VERSION = "0.3.0";

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new JJsonCmd()).execute(args);
        System.exit(exitCode);
    }
}
