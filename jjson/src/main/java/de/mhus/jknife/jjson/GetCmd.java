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
package de.mhus.jknife.jjson;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Extracts a value by a simple path (a.b[0].c or $.a.b).
 */
@Command(name = "get", mixinStandardHelpOptions = true, description = "Get a json value by path (e.g. a.b[0].c or $.a.b). Values print raw, objects/arrays as compact json. Exit code 0 found, 1 not found, 2 on error.")
public class GetCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "PATH", description = "The path to the value, e.g. a.b[0].c or $.a.b")
    private String path;

    @Parameters(index = "1", paramLabel = "JSON", arity = "0..1", description = "The json to process (default: read from stdin)")
    private String json;

    @Mixin
    private TextInput input;

    @Option(names = { "-p", "--pretty" }, description = "Pretty print objects/arrays instead of compact")
    private boolean pretty;

    @Override
    public Integer call() throws Exception {
        JsonNode root;
        try {
            root = JsonUtil.read(input.resolveText(json));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
        JsonNode node;
        try {
            node = JsonUtil.navigate(root, path);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
        if (node == null || node.isMissingNode()) {
            input.verbose("Not found: " + path);
            return 1;
        }
        if (node.isValueNode()) {
            System.out.println(node.asText());
        } else if (pretty) {
            System.out.println(JsonUtil.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(node));
        } else {
            System.out.println(JsonUtil.mapper().writeValueAsString(node));
        }
        return 0;
    }
}
