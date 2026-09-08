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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Timestamp parsing: auto-detects epoch seconds, epoch milliseconds and ISO-8601 / ISO-like date-time formats.
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * Parses a timestamp value into an Instant.
     *
     * Supported: epoch seconds (1-10 digits), epoch milliseconds (11-13 digits), ISO-8601 with offset/Z (Instant,
     * OffsetDateTime, ZonedDateTime), and local date-time (interpreted in the given zone).
     *
     * @param value
     *            the value to parse
     * @param zone
     *            zone used for values without offset (e.g. local date-time)
     *
     * @throws IllegalArgumentException
     *             if the value cannot be parsed
     */
    public static Instant parse(String value, ZoneId zone) {
        value = value.trim();

        // epoch seconds (1-10 digits) / epoch milliseconds (11-13 digits)
        if (value.matches("\\d{1,10}"))
            return Instant.ofEpochSecond(Long.parseLong(value));
        if (value.matches("\\d{11,13}"))
            return Instant.ofEpochMilli(Long.parseLong(value));

        // ISO-8601 variants
        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException ignored) {
        }
        try {
            return ZonedDateTime.parse(value).toInstant();
        } catch (RuntimeException ignored) {
        }
        try {
            return LocalDateTime.parse(value).atZone(zone).toInstant();
        } catch (RuntimeException ignored) {
        }

        throw new IllegalArgumentException("Unrecognized timestamp: '" + value
                + "' (supported: epoch seconds/millis, ISO-8601 like 2024-09-08T09:56:40Z)");
    }

    /**
     * Resolves a zone id string, empty or null means the system default.
     *
     * @throws IllegalArgumentException
     *             for unknown zones
     */
    public static ZoneId zone(String zone) {
        return zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone.trim());
    }
}
