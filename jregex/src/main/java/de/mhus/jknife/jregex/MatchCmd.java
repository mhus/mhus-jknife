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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Tests if the complete text matches the regex (Matcher.matches).
 */
@Command(name = "match", mixinStandardHelpOptions = true, description = "Test if the full text matches the regex. Exit code 0 if matching, 1 if not, 2 on error.")
public class MatchCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "REGEX", description = "The regular expression")
    private String regex;

    @Parameters(index = "1", paramLabel = "TEXT", arity = "0..1", description = "The text to test (default: read from stdin)")
    private String text;

    @Mixin
    private RegexInput input;

    @Option(names = { "-q", "--quiet" }, description = "Do not print the result, only set the exit code")
    private boolean quiet;

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

        boolean matched = pattern.matcher(text).matches();
        input.verbose("Result: " + matched);
        if (!quiet)
            System.out.println(matched);
        return matched ? 0 : 1;
    }
}
