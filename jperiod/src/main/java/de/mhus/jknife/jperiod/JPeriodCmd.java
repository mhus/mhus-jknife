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

import java.time.Duration;
import java.util.concurrent.Callable;

import static de.mhus.jknife.jperiod.DurationUtil.Unit;

/**
 * jperiod - period/duration converter tool.
 *
 * Subcommands: parse, add, sub.
 */
@Command(name = "jperiod", mixinStandardHelpOptions = true, version = "jperiod "
        + JPeriodCmd.VERSION, description = "Period/duration converter and parser tool.", subcommands = {
                CommandLine.HelpCommand.class, ParseCmd.class, AddCmd.class, SubCmd.class })
public class JPeriodCmd implements Callable<Integer> {

    /** keep in sync with the maven project version */
    public static final String VERSION = "0.1.0";

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new JPeriodCmd()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Prints a duration in all representations, or only in the selected unit.
     */
    static void print(Duration duration, Unit onlyUnit) {
        if (onlyUnit != null) {
            System.out.println(DurationUtil.format(duration, onlyUnit));
            return;
        }
        System.out.println("iso: " + duration);
        System.out.println("human: " + DurationUtil.human(duration));
        System.out.println("seconds: " + DurationUtil.format(duration, Unit.SECONDS));
        System.out.println("millis: " + DurationUtil.format(duration, Unit.MILLIS));
        System.out.println("minutes: " + DurationUtil.format(duration, Unit.MINUTES));
        System.out.println("hours: " + DurationUtil.format(duration, Unit.HOURS));
        System.out.println("days: " + DurationUtil.format(duration, Unit.DAYS));
        System.out.println("weeks: " + DurationUtil.format(duration, Unit.WEEKS));
    }
}
