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
package de.mhus.jknife.jregex;

import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine.Option;

/**
 * Common options for all jregex subcommands: text input (inherited) plus regex flags.
 */
public class RegexInput extends TextInput {

    @Option(names = { "-f", "--flags" }, description = "Regex flags, comma separated: " + RegexFlags.FLAG_NAMES)
    private String flagsStr = "";

    public int flags() {
        return RegexFlags.parse(flagsStr);
    }

    public String flagsStr() {
        return flagsStr;
    }
}
