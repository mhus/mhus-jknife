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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Replaces all (or the first) regex matches in the text.
 */
@Command(name = "replace", mixinStandardHelpOptions = true, description = "Replace regex matches in the text and print the result. Supports $1..$n group references in the replacement.")
public class ReplaceCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "REGEX", description = "The regular expression")
    private String regex;

    @Parameters(index = "1", paramLabel = "REPLACEMENT", description = "The replacement (supports $1..$n group references)")
    private String replacement;

    @Parameters(index = "2", paramLabel = "TEXT", arity = "0..1", description = "The text to process (default: read from stdin)")
    private String text;

    @Mixin
    private RegexInput input;

    @Option(names = { "--first" }, description = "Replace only the first match instead of all")
    private boolean first;

    @Override
    public Integer call() throws Exception {
        String text = input.resolveText(this.text);
        input.verbose("Text: " + text);
        input.verbose("Pattern: " + regex);
        input.verbose("Flags: " + input.flagsStr());

        Pattern pattern;
        try {
            pattern = Pattern.compile(regex, input.flags());
        } catch (PatternSyntaxException e) {
            System.err.println("Invalid regex: " + e.getMessage());
            return 2;
        }

        try {
            String result = first ? pattern.matcher(text).replaceFirst(replacement)
                    : pattern.matcher(text).replaceAll(replacement);
            input.verbose("Result: " + result);
            System.out.println(result);
            return 0;
        } catch (RuntimeException e) {
            System.err.println("Replace failed: " + e.getMessage());
            return 2;
        }
    }
}
