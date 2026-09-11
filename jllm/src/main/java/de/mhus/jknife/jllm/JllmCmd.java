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
package de.mhus.jknife.jllm;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * jllm - LLM tool family (based on langchain4j, providers: openai, ollama).
 *
 * Subcommands: ask (complete response), stream (live tokens), request (raw json passthrough), models (discovery).
 */
@Command(name = "jllm", mixinStandardHelpOptions = true, version = "jllm "
        + JllmCmd.VERSION, description = "LLM tool family based on langchain4j (providers: openai, ollama).", subcommands = {
                CommandLine.HelpCommand.class, AskCmd.class, StreamCmd.class, RequestCmd.class, ModelsCmd.class })
public class JllmCmd implements Callable<Integer> {

    /** keep in sync with the maven project version */
    public static final String VERSION = "0.3.0";

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new JllmCmd()).execute(args);
        System.exit(exitCode);
    }
}
