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

import de.mhus.jknife.jllm.config.LlmConfig;
import de.mhus.jknife.jllm.config.LlmConfigLoader;
import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Raw request: sends a provider specific json body (e.g. the openai chat completion json) directly to the provider
 * endpoint and streams the raw response to stdout, byte for byte. No langchain4j, no parsing - curl for llms.
 */
@Command(name = "request", mixinStandardHelpOptions = true, description = "Raw llm request: send a provider specific json body (e.g. the openai chat completion json) directly to the provider endpoint and stream the raw response to stdout. Exit code 0 on success, 2 on error.")
public class RequestCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "BODY", arity = "0..1", description = "The request body, provider specific json (default: read from stdin)")
    private String body;

    @Mixin
    private TextInput input;

    @Option(names = {
            "--llm" }, paramLabel = "CONFIG", description = "Inline llm config, yaml or json. Only provider, baseUrl, apiKey and timeoutSeconds are used. Merged over --llm-config.")
    private String llmInline;

    @Option(names = { "--llm-config" }, paramLabel = "FILE", description = "Llm config file (yaml or json)")
    private String llmConfigFile;

    @Override
    public Integer call() throws Exception {
        LlmConfig config;
        try {
            config = LlmConfigLoader.load(llmInline, llmConfigFile);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        byte[] body = input.resolveBytes(this.body);
        if (body == null || body.length == 0) {
            System.err.println("Empty request body");
            return 2;
        }

        String endpoint = endpoint(config);
        input.verbose("Endpoint: " + endpoint);

        try {
            var requestBuilder = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds())).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            if (LlmConfig.PROVIDER_OPENAI.equals(config.provider()))
                requestBuilder.header("Authorization", "Bearer " + config.apiKey());

            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(config.timeoutSeconds())).build()
                    .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() / 100 != 2) {
                var errorBody = new String(response.body().readAllBytes(), input.charset());
                System.err.println("HTTP " + response.statusCode() + " from " + endpoint);
                System.err.println(errorBody);
                return 2;
            }

            // stream the raw response to stdout as it arrives (works for json and sse)
            try (var in = response.body()) {
                in.transferTo(System.out);
            }
            System.out.flush();
            return 0;
        } catch (java.io.IOException e) {
            System.err.println("Llm request failed: " + firstLine(e.getMessage()));
            input.verbose(e.toString());
            return 2;
        }
    }

    /**
     * Provider default endpoints: openai baseUrl + /chat/completions, ollama baseUrl + /api/chat. A body with "stream":
     * true makes the provider answer with a live stream, which is passed through as it arrives.
     */
    private static String endpoint(LlmConfig config) {
        String base = config.baseUrl().replaceAll("/+$", "");
        return switch (config.provider()) {
        case LlmConfig.PROVIDER_OPENAI -> base + "/chat/completions";
        case LlmConfig.PROVIDER_OLLAMA -> base + "/api/chat";
        default -> throw new IllegalArgumentException("Unknown llm provider: " + config.provider());
        };
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }
}
