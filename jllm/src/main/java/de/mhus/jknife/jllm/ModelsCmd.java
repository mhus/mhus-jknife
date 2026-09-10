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

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.jknife.jllm.config.LlmConfig;
import de.mhus.jknife.jllm.config.LlmConfigLoader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Model discovery: lists the available models or shows the details of one model. openai: GET <baseUrl>/models and GET
 * <baseUrl>/models/{model}; ollama: GET <baseUrl>/api/tags and POST <baseUrl>/api/show.
 */
@Command(name = "models", mixinStandardHelpOptions = true, description = "List the available models (no argument) or show the details of one model. Exit code 0 on success, 2 on error.")
public class ModelsCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "MODEL", arity = "0..1", description = "Model to show details for (default: list all models)")
    private String model;

    @Option(names = {
            "--llm" }, paramLabel = "CONFIG", description = "Inline llm config, yaml or json. Only provider, baseUrl, apiKey and timeoutSeconds are used. Merged over --llm-config.")
    private String llmInline;

    @Option(names = { "--llm-config" }, paramLabel = "FILE", description = "Llm config file (yaml or json)")
    private String llmConfigFile;

    @Option(names = { "-v", "--verbose" }, description = "List with details (owner, family, size, quantization)")
    private boolean verbose;

    @Option(names = { "-r", "--raw" }, description = "Print the raw endpoint response, byte for byte")
    private boolean raw;

    @Override
    public Integer call() throws Exception {
        LlmConfig config;
        try {
            config = LlmConfigLoader.load(llmInline, llmConfigFile);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        String base = config.baseUrl().replaceAll("/+$", "");
        String endpoint;
        String method = "GET";
        byte[] body = null;
        if (model == null || model.isBlank()) {
            // list
            endpoint = LlmConfig.PROVIDER_OPENAI.equals(config.provider()) ? base + "/models" : base + "/api/tags";
        } else {
            // info
            if (LlmConfig.PROVIDER_OPENAI.equals(config.provider())) {
                endpoint = base + "/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8);
            } else {
                endpoint = base + "/api/show";
                method = "POST";
                body = ("{\"model\":\"" + model + "\"}").getBytes(StandardCharsets.UTF_8);
            }
        }

        try {
            var requestBuilder = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds())).header("Content-Type", "application/json");
            if (LlmConfig.PROVIDER_OPENAI.equals(config.provider()))
                requestBuilder.header("Authorization", "Bearer " + config.apiKey());
            if ("POST".equals(method))
                requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
            else
                requestBuilder.GET();

            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(config.timeoutSeconds())).build()
                    .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() / 100 != 2) {
                System.err.println("HTTP " + response.statusCode() + " from " + endpoint);
                System.err.println(new String(response.body(), StandardCharsets.UTF_8));
                return 2;
            }

            var bytes = response.body();
            if (raw) {
                System.out.write(bytes);
                System.out.flush();
                return 0;
            }

            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes);
            if (model == null || model.isBlank())
                printList(config, json);
            else
                printInfo(json);
            return 0;
        } catch (java.io.IOException e) {
            System.err.println("Model discovery failed: " + firstLine(e.getMessage()));
            return 2;
        }
    }

    private void printList(LlmConfig config, JsonNode json) {
        boolean openai = LlmConfig.PROVIDER_OPENAI.equals(config.provider());
        JsonNode entries = openai ? json.get("data") : json.get("models");
        if (entries == null || !entries.isArray() || entries.isEmpty()) {
            System.err.println("No models found");
            return;
        }
        for (JsonNode entry : entries) {
            String name = entry.path(openai ? "id" : "name").asText();
            if (!verbose) {
                System.out.println(name);
                continue;
            }
            var sb = new StringBuilder(name);
            if (openai) {
                if (entry.hasNonNull("owned_by"))
                    sb.append(" | owned_by=").append(entry.get("owned_by").asText());
                if (entry.hasNonNull("created"))
                    sb.append(" | created=").append(java.time.Instant.ofEpochSecond(entry.get("created").asLong()));
            } else {
                var details = entry.get("details");
                if (details != null) {
                    if (details.hasNonNull("family"))
                        sb.append(" | family=").append(details.get("family").asText());
                    if (details.hasNonNull("parameter_size"))
                        sb.append(" | params=").append(details.get("parameter_size").asText());
                    if (details.hasNonNull("quantization_level"))
                        sb.append(" | quant=").append(details.get("quantization_level").asText());
                }
                if (entry.hasNonNull("size"))
                    sb.append(" | size=").append(humanReadable(entry.get("size").asLong()));
                if (entry.hasNonNull("modified_at"))
                    sb.append(" | modified=").append(entry.get("modified_at").asText());
            }
            System.out.println(sb);
        }
    }

    private void printInfo(JsonNode json) throws java.io.IOException {
        System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(json));
    }

    private static String humanReadable(long bytes) {
        if (bytes < 1024)
            return bytes + "B";
        double value = bytes;
        for (String unit : new String[] { "KB", "MB", "GB", "TB" }) {
            value /= 1024;
            if (value < 1024)
                return String.format("%.1f%s", value, unit);
        }
        return String.format("%.1fPB", value / 1024);
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }
}
