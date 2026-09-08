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

import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Compacts (minifies) json.
 */
@Command(name = "compact", mixinStandardHelpOptions = true, description = "Compact (minify) json. Exit code 0 on success, 2 on error.")
public class CompactCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "JSON", arity = "0..1", description = "The json to process (default: read from stdin)")
    private String json;

    @Mixin
    private TextInput input;

    @Override
    public Integer call() throws Exception {
        try {
            var node = JsonUtil.read(input.resolveText(json));
            System.out.println(JsonUtil.mapper().writeValueAsString(node));
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }
}
