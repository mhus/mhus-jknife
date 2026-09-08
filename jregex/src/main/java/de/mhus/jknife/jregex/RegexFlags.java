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

import java.util.regex.Pattern;

/**
 * Parses comma separated regex flag names into a java.util.regex Pattern flag bitmask.
 */
public final class RegexFlags {

    public static final String FLAG_NAMES = "CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS";

    private RegexFlags() {
    }

    public static int parse(String csv) {
        if (csv == null || csv.isBlank())
            return 0;
        int flags = 0;
        for (String part : csv.toUpperCase().split(",")) {
            part = part.strip();
            if (part.isEmpty())
                continue;
            flags |= flagOf(part);
        }
        return flags;
    }

    private static int flagOf(String name) {
        return switch (name) {
        case "CASE_INSENSITIVE" -> Pattern.CASE_INSENSITIVE;
        case "MULTILINE" -> Pattern.MULTILINE;
        case "DOTALL" -> Pattern.DOTALL;
        case "UNICODE_CASE" -> Pattern.UNICODE_CASE;
        case "CANON_EQ" -> Pattern.CANON_EQ;
        case "UNIX_LINES" -> Pattern.UNIX_LINES;
        case "LITERAL" -> Pattern.LITERAL;
        case "UNICODE_CHARACTER_CLASS" -> Pattern.UNICODE_CHARACTER_CLASS;
        case "COMMENTS" -> Pattern.COMMENTS;
        default -> throw new IllegalArgumentException("Unknown regex flag: " + name + " (allowed: " + FLAG_NAMES + ")");
        };
    }
}
