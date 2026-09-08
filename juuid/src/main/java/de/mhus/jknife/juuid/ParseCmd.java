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
package de.mhus.jknife.juuid;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Parses a UUID and prints its properties (version, variant, v7 timestamp).
 */
@Command(name = "parse", mixinStandardHelpOptions = true, description = "Parse a UUID and print its properties. Exit code 0 on success, 2 on error.")
public class ParseCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "UUID", description = "The UUID to parse")
    private String uuidStr;

    @Override
    public Integer call() {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID: " + e.getMessage());
            return 2;
        }

        System.out.println("value: " + uuid);
        System.out.println("version: " + uuid.version());
        System.out.println("variant: " + variantName(uuid.variant()));
        if (uuid.version() == 7) {
            var ts = UuidUtil.v7Timestamp(uuid);
            System.out.println(
                    "timestamp: " + ts.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return 0;
    }

    private static String variantName(int variant) {
        return switch (variant) {
        case 0 -> "0 (reserved, NCS backward compatibility)";
        case 2 -> "2 (RFC 4122 / RFC 9562)";
        case 6 -> "6 (reserved, Microsoft)";
        case 7 -> "7 (reserved, future definition)";
        default -> variant + " (invalid)";
        };
    }
}
