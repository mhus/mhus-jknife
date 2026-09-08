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
import java.util.List;
import java.util.concurrent.Callable;

import static de.mhus.jknife.jperiod.DurationUtil.Unit;

/**
 * Sums up durations.
 */
@Command(name = "add", mixinStandardHelpOptions = true, description = "Add up durations (e.g. jperiod add 2h30m 45m 1h) and print the sum. Exit code 0 on success, 2 on error.")
public class AddCmd implements Callable<Integer> {

    @Parameters(index = "0", arity = "1..*", paramLabel = "DURATION", description = "The durations to add, e.g. 2h30m 45m or P1D PT12H")
    private List<String> durations;

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

        try {
            var sum = Duration.ZERO;
            for (String duration : durations)
                sum = sum.plus(DurationUtil.parse(duration));
            JPeriodCmd.print(sum, onlyUnit);
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }
}
