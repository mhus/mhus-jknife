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
import picocli.CommandLine.Option;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Generates UUIDs, one per line.
 */
@Command(name = "generate", aliases = "gen", mixinStandardHelpOptions = true, description = "Generate UUIDs, one per line. Exit code 0 on success, 2 on error.")
public class GenerateCmd implements Callable<Integer> {

    @Option(names = { "-n",
            "--count" }, paramLabel = "N", defaultValue = "1", description = "Number of UUIDs to generate (default: ${DEFAULT-VALUE})")
    private int count;

    @Option(names = { "-t",
            "--type" }, paramLabel = "VERSION", defaultValue = "4", description = "UUID version: 4 (random, default) or 7 (time sortable)")
    private int type;

    @Option(names = { "--upper" }, description = "Print uppercase UUIDs")
    private boolean upper;

    @Override
    public Integer call() {
        if (type != 4 && type != 7) {
            System.err.println("Invalid UUID version: " + type + " (allowed: 4, 7)");
            return 2;
        }
        if (count < 1) {
            System.err.println("Count must be >= 1");
            return 2;
        }
        for (int i = 0; i < count; i++) {
            UUID uuid = type == 7 ? UuidUtil.generateV7() : UUID.randomUUID();
            String value = upper ? uuid.toString().toUpperCase() : uuid.toString();
            System.out.println(value);
        }
        return 0;
    }
}
