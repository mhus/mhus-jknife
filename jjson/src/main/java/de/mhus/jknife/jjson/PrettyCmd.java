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
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Pretty prints json.
 */
@Command(name = "pretty", mixinStandardHelpOptions = true, description = "Pretty print (indent) json. Exit code 0 on success, 2 on error.")
public class PrettyCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "JSON", arity = "0..1", description = "The json to process (default: read from stdin)")
    private String json;

    @Mixin
    private TextInput input;

    @Option(names = { "-i",
            "--indent" }, paramLabel = "N", defaultValue = "2", description = "Indentation width (default: ${DEFAULT-VALUE})")
    private int indent;

    @Override
    public Integer call() throws Exception {
        String json;
        JsonNode node;
        try {
            json = input.resolveText(this.json);
            node = JsonUtil.read(json);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
        var indenter = new DefaultIndenter(" ".repeat(Math.max(0, indent)), DefaultIndenter.SYS_LF);
        var printer = new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter);
        System.out.println(JsonUtil.mapper().writer(printer).writeValueAsString(node));
        return 0;
    }
}
