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
package de.mhus.jknife.jtime;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

/**
 * Prints the current time in the selected format.
 */
@Command(name = "now", mixinStandardHelpOptions = true, description = "Print the current time. Default: ISO-8601 with offset in the given zone. Exit code 0 on success, 2 on error.")
public class NowCmd implements Callable<Integer> {

    @Option(names = { "-z",
            "--zone" }, paramLabel = "ZONE", description = "Time zone id, e.g. UTC or Europe/Berlin (default: system zone)")
    private String zoneStr;

    @Option(names = { "--epoch" }, description = "Print unix epoch seconds")
    private boolean epoch;

    @Option(names = { "--millis" }, description = "Print unix epoch milliseconds")
    private boolean millis;

    @Option(names = { "-f",
            "--format" }, paramLabel = "PATTERN", description = "Print in a custom java time pattern, e.g. 'yyyy-MM-dd HH:mm:ss'")
    private String pattern;

    @Override
    public Integer call() {
        if (epoch && millis || epoch && pattern != null || millis && pattern != null) {
            System.err.println("Use only one of --epoch, --millis, --format");
            return 2;
        }

        ZoneId zone;
        try {
            zone = TimeUtil.zone(zoneStr);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        try {
            Instant now = Instant.now();
            if (epoch) {
                System.out.println(now.getEpochSecond());
            } else if (millis) {
                System.out.println(now.toEpochMilli());
            } else if (pattern != null) {
                System.out.println(DateTimeFormatter.ofPattern(pattern).withZone(zone).format(now));
            } else {
                System.out.println(ZonedDateTime.ofInstant(now, zone));
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid format pattern: " + e.getMessage());
            return 2;
        }
    }
}
