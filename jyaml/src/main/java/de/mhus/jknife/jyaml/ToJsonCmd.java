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
package de.mhus.jknife.jyaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Converts yaml to json.
 */
@Command(name = "tojson", mixinStandardHelpOptions = true, description = "Convert yaml to json. Exit code 0 on success, 2 on error.")
public class ToJsonCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "YAML", arity = "0..1", description = "The yaml to convert (default: read from stdin)")
    private String yaml;

    @Mixin
    private TextInput input;

    @Option(names = { "-p", "--pretty" }, description = "Pretty print the json instead of compact")
    private boolean pretty;

    @Override
    public Integer call() throws Exception {
        try {
            Object value = YamlUtil.read(input.resolveText(yaml));
            var node = YamlUtil.toJson(value);
            var writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            System.out.println(pretty ? writer.writeValueAsString(node) : new ObjectMapper().writeValueAsString(node));
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }
}
