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
 * Finds all regex matches in the text (Matcher.find loop) and prints them.
 */
@Command(name = "find", mixinStandardHelpOptions = true, description = "Find all regex matches in the text and print one match per line. Exit code 0 if at least one match, 1 if none, 2 on error.")
public class FindCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "REGEX", description = "The regular expression")
    private String regex;

    @Parameters(index = "1", paramLabel = "TEXT", arity = "0..1", description = "The text to search in (default: read from stdin)")
    private String text;

    @Mixin
    private RegexInput input;

    @Option(names = { "-g",
            "--group" }, paramLabel = "INDEX", defaultValue = "-1", description = "Print the given capture group instead of the whole match (0 = whole match, 1..n = groups)")
    private int group;

    @Option(names = { "--count" }, description = "Print only the number of matches")
    private boolean count;

    @Option(names = { "-n",
            "--line-number" }, description = "Prefix each printed match with its line number in the input text")
    private boolean lineNumber;

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

        Matcher matcher = pattern.matcher(text);
        int found = 0;
        while (matcher.find()) {
            found++;
            if (count)
                continue;
            String value;
            if (group >= 0) {
                try {
                    value = matcher.group(group);
                } catch (IndexOutOfBoundsException e) {
                    System.err.println(
                            "Match " + found + " has no group " + group + " (groups: " + matcher.groupCount() + ")");
                    return 2;
                }
            } else {
                value = matcher.group();
            }
            if (value == null)
                value = "";
            if (lineNumber)
                System.out.println(lineOf(text, matcher.start()) + ":" + value);
            else
                System.out.println(value);
        }
        input.verbose("Matches: " + found);
        if (count)
            System.out.println(found);
        return found > 0 ? 0 : 1;
    }

    private static int lineOf(String text, int pos) {
        int line = 1;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n')
                line++;
        }
        return line;
    }
}
