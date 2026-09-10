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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for 'jllm models' against fake discovery endpoints.
 */
class JllmModelsCliTest {

    /** path based fake rest server: maps path -> response body */
    static class FakeRestServer {

        final HttpServer server;
        final List<String> requestPaths = new CopyOnWriteArrayList<>();
        final List<String> requestBodies = new CopyOnWriteArrayList<>();

        FakeRestServer(Map<String, String> pathResponses) throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                requestPaths.add(exchange.getRequestURI().getPath());
                requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                var body = pathResponses.getOrDefault(exchange.getRequestURI().getPath(),
                        "{\"error\":\"unknown path\"}");
                var bytes = body.getBytes(StandardCharsets.UTF_8);
                var status = pathResponses.containsKey(exchange.getRequestURI().getPath()) ? 200 : 404;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        void stop() {
            server.stop(0);
        }
    }

    private static final String OPENAI_MODELS = """
            {"object":"list","data":[
              {"id":"gpt-4o-mini","object":"model","created":1725787000,"owned_by":"system"},
              {"id":"gpt-4o","object":"model","created":1725787100,"owned_by":"openai"}
            ]}
            """;

    private static final String OLLAMA_TAGS = """
            {"models":[
              {"name":"llama3.1:latest","model":"llama3.1","modified_at":"2024-09-01T12:00:00Z","size":4944013824,
               "details":{"family":"llama","parameter_size":"8.0B","quantization_level":"Q4_K_M"}},
              {"name":"qwen2:1.5b","model":"qwen2","modified_at":"2024-08-01T12:00:00Z","size":935198000,
               "details":{"family":"qwen2","parameter_size":"1.5B","quantization_level":"Q4_0"}}
            ]}
            """;

    private static final String OLLAMA_SHOW = """
            {"license":"llama3.1 license",
             "modelfile":"FROM llama3.1",
             "parameters":"stop \\"<|start_header_id|>\\"",
             "template":"{{ .Prompt }}",
             "details":{"family":"llama","parameter_size":"8.0B","quantization_level":"Q4_K_M"},
             "model_info":{"llama.context_length":131072}}
            """;

    private record Result(int exitCode, String out, String err) {
    }

    private final List<FakeRestServer> servers = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(FakeRestServer::stop);
    }

    private Result run(String... args) {
        var outBuffer = new ByteArrayOutputStream();
        var errBuffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        var oldErr = System.err;
        try {
            System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JllmCmd()).execute(args);
            return new Result(exit, new String(outBuffer.toByteArray(), StandardCharsets.UTF_8),
                    new String(errBuffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    private FakeRestServer start(Map<String, String> paths) throws IOException {
        var server = new FakeRestServer(paths);
        servers.add(server);
        return server;
    }

    @Test
    void listOpenAiModels() throws Exception {
        var server = start(Map.of("/v1/models", OPENAI_MODELS));
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("models", "--llm", config);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).containsExactly("gpt-4o-mini", "gpt-4o");

        var verbose = run("models", "--llm", config, "-v");
        assertThat(verbose.out().lines().toList()).containsExactlyInAnyOrder(
                "gpt-4o-mini | owned_by=system | created=2024-09-08T09:16:40Z",
                "gpt-4o | owned_by=openai | created=2024-09-08T09:18:20Z");
    }

    @Test
    void listOllamaModels() throws Exception {
        var server = start(Map.of("/api/tags", OLLAMA_TAGS));
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("models", "--llm", config);
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().lines().toList()).containsExactly("llama3.1:latest", "qwen2:1.5b");

        var verbose = run("models", "--llm", config, "-v");
        assertThat(verbose.out()).contains("llama3.1:latest | family=llama | params=8.0B | quant=Q4_K_M");
        assertThat(verbose.out()).contains("qwen2:1.5b | family=qwen2 | params=1.5B | quant=Q4_0 | size=891.9MB");
    }

    @Test
    void ollamaModelInfo() throws Exception {
        var server = start(Map.of("/api/show", OLLAMA_SHOW));
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("models", "--llm", config, "llama3.1:latest");
        assertThat(r.exitCode()).isZero();
        // pretty json info, contains license and context length
        assertThat(r.out()).contains("\"license\" : \"llama3.1 license\"").contains("131072");
        // the model name was asked via POST /api/show
        assertThat(server.requestPaths).containsExactly("/api/show");
        assertThat(server.requestBodies.get(0)).contains("llama3.1:latest");
    }

    @Test
    void openAiModelInfo() throws Exception {
        var server = start(Map.of("/v1/models/gpt-4o-mini",
                "{\"id\":\"gpt-4o-mini\",\"object\":\"model\",\"created\":1725787000,\"owned_by\":\"system\"}"));
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("models", "--llm", config, "gpt-4o-mini");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("\"id\" : \"gpt-4o-mini\"").contains("\"owned_by\" : \"system\"");
    }

    @Test
    void rawPassthrough() throws Exception {
        var server = start(Map.of("/api/tags", OLLAMA_TAGS));
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("models", "--llm", config, "--raw");
        assertThat(r.exitCode()).isZero();
        // byte exact passthrough
        assertThat(r.out()).isEqualTo(OLLAMA_TAGS);
    }

    @Test
    void unknownModelHttpError() throws Exception {
        var server = start(Map.of("/api/show", "{\"error\":\"model not found\"}"));
        // fake returns 404 for unknown path
        var server404 = start(Map.of("/api/other", "{}"));
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server404.baseUrl() + "'}";

        var r = run("models", "--llm", config, "unknown-model");
        assertThat(r.exitCode()).isEqualTo(2);
        assertThat(r.err()).contains("HTTP 404");
        assertThat(r.out()).isEmpty();
    }
}
