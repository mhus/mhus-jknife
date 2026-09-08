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
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

/**
 * Parses a timestamp (epoch, epoch-millis or ISO-8601) and prints it in all common representations.
 */
@Command(name = "parse", mixinStandardHelpOptions = true, description = "Parse a timestamp (epoch seconds, epoch millis or ISO-8601) and print all representations of it. Exit code 0 on success, 2 on error.")
public class ParseCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "VALUE", description = "The timestamp to parse, e.g. 1725787000, 1725787000123 or 2024-09-08T09:56:40Z")
    private String value;

    @Option(names = { "-z",
            "--zone" }, paramLabel = "ZONE", description = "Time zone for ISO output and for values without offset, e.g. Europe/Berlin (default: system zone)")
    private String zoneStr;

    @Option(names = { "-f",
            "--format" }, paramLabel = "PATTERN", description = "Print in a custom java time pattern instead of all representations, e.g. 'yyyy-MM-dd HH:mm:ss'")
    private String pattern;

    @Override
    public Integer call() {
        ZoneId zone;
        try {
            zone = TimeUtil.zone(zoneStr);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        Instant instant;
        try {
            instant = TimeUtil.parse(value, zone);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        try {
            if (pattern != null) {
                System.out.println(DateTimeFormatter.ofPattern(pattern).withZone(zone).format(instant));
            } else {
                System.out.println("epoch: " + instant.getEpochSecond());
                System.out.println("epoch-millis: " + instant.toEpochMilli());
                System.out.println("iso-utc: " + instant);
                System.out.println("iso[" + zone + "]: " + ZonedDateTime.ofInstant(instant, zone));
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid format pattern: " + e.getMessage());
            return 2;
        }
    }
}
