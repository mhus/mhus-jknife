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
package de.mhus.jknife.jperiod;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.util.concurrent.Callable;

import static de.mhus.jknife.jperiod.DurationUtil.Unit;

/**
 * Parses a duration and prints it in all representations.
 */
@Command(name = "parse", mixinStandardHelpOptions = true, description = "Parse a duration (ISO-8601 like P1DT2H30M or shorthand like 2h30m, 90s, 1.5d) and print all representations of it. A bare number means seconds. Exit code 0 on success, 2 on error.")
public class ParseCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "DURATION", description = "The duration to parse, e.g. 2h30m, 90s, 1.5d or P1DT2H30M")
    private String value;

    @Option(names = { "-u",
            "--unit" }, paramLabel = "UNIT", description = "Print only the value in this unit: ns, us, ms, s, m, h, d, w (long forms like 'minutes' work too)")
    private String unitStr;

    @Override
    public Integer call() {
        Unit onlyUnit = null;
        try {
            if (unitStr != null)
                onlyUnit = DurationUtil.unit(unitStr);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        Duration duration;
        try {
            duration = DurationUtil.parse(value);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        JPeriodCmd.print(duration, onlyUnit);
        return 0;
    }
}
