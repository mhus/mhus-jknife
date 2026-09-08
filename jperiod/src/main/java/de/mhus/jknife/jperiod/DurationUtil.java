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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Duration helpers: parsing of ISO-8601 durations and shorthand durations ("2h30m", "90s", "1.5d"), human readable
 * formatting and unit conversion.
 */
public final class DurationUtil {

    /** time units with their exact length in nanoseconds */
    public enum Unit {
        NANOS("ns", 1L), MICROS("us", 1_000L), MILLIS("ms", 1_000_000L), SECONDS("s", 1_000_000_000L),
        MINUTES("m", 60_000_000_000L), HOURS("h", 3_600_000_000_000L), DAYS("d", 86_400_000_000_000L),
        WEEKS("w", 604_800_000_000_000L);

        final String shortName;
        final long nanos;

        Unit(String shortName, long nanos) {
            this.shortName = shortName;
            this.nanos = nanos;
        }
    }

    private static final Map<String, Unit> UNITS = createUnits();

    private static Map<String, Unit> createUnits() {
        var units = new java.util.HashMap<String, Unit>();
        for (Unit unit : Unit.values()) {
            units.put(unit.shortName, unit);
            units.put(unit.name().toLowerCase(), unit);
        }
        units.put("min", Unit.MINUTES);
        return Map.copyOf(units);
    }

    private static final Pattern ISO_PATTERN = Pattern.compile(
            "P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORTHAND_TOKEN = Pattern.compile("(\\d+(?:\\.\\d+)?)(ns|us|ms|s|m|h|d|w)");
    private static final Pattern BARE_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private DurationUtil() {
    }

    /**
     * Parses a duration: ISO-8601 (P1DT2H30M), shorthand (2h30m, 90s, 1.5d) or a bare number (interpreted as seconds).
     * Years and months are rejected because their length is ambiguous.
     *
     * @throws IllegalArgumentException
     *             if the input cannot be parsed
     */
    public static Duration parse(String input) {
        if (input == null)
            throw new IllegalArgumentException("Empty duration");
        input = input.trim().replace(',', '.');
        if (input.isEmpty())
            throw new IllegalArgumentException("Empty duration");

        boolean negative = input.startsWith("-");
        if (negative)
            input = input.substring(1);

        Duration duration = input.toUpperCase().startsWith("P") ? parseIso(input) : parseShorthand(input);
        return negative ? duration.negated() : duration;
    }

    private static Duration parseIso(String input) {
        Matcher m = ISO_PATTERN.matcher(input);
        if (!m.matches() || m.group(1) != null || m.group(2) != null)
            throw new IllegalArgumentException("Invalid ISO-8601 duration: '" + input
                    + "' (years and months are not supported, convert to days first)");
        long weeks = parseLong(m.group(3));
        long days = parseLong(m.group(4));
        long hours = parseLong(m.group(5));
        long minutes = parseLong(m.group(6));
        BigDecimal seconds = m.group(7) != null ? new BigDecimal(m.group(7)) : BigDecimal.ZERO;
        try {
            return Duration.ofDays(days).plusDays(weeks * 7).plusHours(hours).plusMinutes(minutes)
                    .plusNanos(seconds.movePointRight(9).longValueExact());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Duration too large: " + input);
        }
    }

    private static Duration parseShorthand(String input) {
        // bare number means seconds
        if (BARE_NUMBER.matcher(input).matches())
            return secondsToDuration(new BigDecimal(input));

        long nanos = 0;
        Matcher m = SHORTHAND_TOKEN.matcher(input);
        while (m.find()) {
            nanos = Math.addExact(nanos, toNanos(new BigDecimal(m.group(1)), unit(m.group(2))));
        }
        String rest = SHORTHAND_TOKEN.matcher(input).replaceAll("").trim();
        if (!rest.isEmpty())
            throw new IllegalArgumentException("Unrecognized duration: '" + input
                    + "' (supported: ISO-8601 like P1DT2H30M or shorthand like 2h30m, 90s, 1.5d)");
        return Duration.ofNanos(nanos);
    }

    private static Duration secondsToDuration(BigDecimal seconds) {
        try {
            return Duration.ofNanos(seconds.movePointRight(9).longValueExact());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Duration too large: " + seconds);
        }
    }

    private static long toNanos(BigDecimal number, Unit unit) {
        try {
            return number.multiply(BigDecimal.valueOf(unit.nanos)).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Duration too large: " + number + unit.shortName);
        }
    }

    private static long parseLong(String value) {
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * Resolves a unit by short ("ns", "s", "m", "h", "d", "w") or long ("seconds", "minutes", ...) name.
     *
     * @throws IllegalArgumentException
     *             for unknown units
     */
    public static Unit unit(String name) {
        Unit unit = UNITS.get(name == null ? "" : name.trim().toLowerCase());
        if (unit == null)
            throw new IllegalArgumentException("Unknown unit: '" + name + "' (allowed: ns, us, ms, s, m, h, d, w)");
        return unit;
    }

    /**
     * Formats the duration in the given unit as exact decimal number (e.g. "150", "2.5").
     */
    public static String format(Duration duration, Unit unit) {
        return BigDecimal.valueOf(duration.toNanos()).divide(BigDecimal.valueOf(unit.nanos), 12, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    /**
     * Human readable breakdown, e.g. "1d 2h 30m", "45s", "1m 30.5s".
     */
    public static String human(Duration duration) {
        if (duration.isZero())
            return "0s";

        boolean negative = duration.isNegative();
        long nanos = (negative ? duration.negated() : duration).toNanos();

        var sb = new StringBuilder();
        if (negative)
            sb.append('-');
        long days = nanos / Unit.DAYS.nanos;
        nanos %= Unit.DAYS.nanos;
        long hours = nanos / Unit.HOURS.nanos;
        nanos %= Unit.HOURS.nanos;
        long minutes = nanos / Unit.MINUTES.nanos;
        nanos %= Unit.MINUTES.nanos;
        long seconds = nanos / Unit.SECONDS.nanos;
        nanos %= Unit.SECONDS.nanos;

        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0)
            sb.append(minutes).append("m ");
        if (seconds > 0 || nanos > 0 || sb.length() == (negative ? 1 : 0)) {
            sb.append(seconds);
            if (nanos > 0) {
                // 9 digit zero padded nanosecond fraction, trailing zeros stripped
                String fraction = String.format("%09d", nanos).replaceAll("0+$", "");
                sb.append('.').append(fraction);
            }
            sb.append('s');
        }
        return sb.toString().trim();
    }
}
