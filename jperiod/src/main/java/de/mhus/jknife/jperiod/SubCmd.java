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
 * Subtracts durations from the first one (result may be negative).
 */
@Command(name = "sub", mixinStandardHelpOptions = true, description = "Subtract durations from the first one (e.g. jperiod sub 5h 90m) and print the result. The result may be negative. Exit code 0 on success, 2 on error.")
public class SubCmd implements Callable<Integer> {

    @Parameters(index = "0", arity = "1..*", paramLabel = "DURATION", description = "First duration minus the remaining ones, e.g. 5h 90m")
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
            var result = DurationUtil.parse(durations.get(0));
            for (int i = 1; i < durations.size(); i++)
                result = result.minus(DurationUtil.parse(durations.get(i)));
            JPeriodCmd.print(result, onlyUnit);
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }
}
