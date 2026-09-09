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
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end tests against fake openai/ollama http endpoints, no network or api key needed.
 */
class JllmRequestCliTest {

    /** minimal fake server speaking enough openai/ollama protocol for one request */
    static class FakeLlmServer {

        final HttpServer server;
        final List<String> requestBodies = new CopyOnWriteArrayList<>();

        FakeLlmServer(String responseBody) throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requestBodies.add(body);
                var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
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

    private static final String OPENAI_RESPONSE = """
            {
              "id": "chatcmpl-test",
              "object": "chat.completion",
              "created": 1725787000,
              "model": "gpt-4o-mini",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "Hello from fake openai!"},
                "finish_reason": "stop"
              }],
              "usage": {"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12}
            }
            """;

    private static final String OLLAMA_RESPONSE = """
            {
              "model": "llama3.1",
              "created_at": "2024-09-08T12:00:00Z",
              "message": {"role": "assistant", "content": "Hello from fake ollama!"},
              "done": true
            }
            """;

    private record Result(int exitCode, String out) {
    }

    private final List<FakeLlmServer> servers = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(FakeLlmServer::stop);
    }

    private Result run(String... args) {
        var buffer = new ByteArrayOutputStream();
        var oldOut = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            int exit = new CommandLine(new JllmCmd()).execute(args);
            return new Result(exit, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(oldOut);
        }
    }

    private FakeLlmServer startOpenAi() throws IOException {
        var server = new FakeLlmServer(OPENAI_RESPONSE);
        servers.add(server);
        return server;
    }

    private FakeLlmServer startOllama() throws IOException {
        var server = new FakeLlmServer(OLLAMA_RESPONSE);
        servers.add(server);
        return server;
    }

    @Test
    void requestOpenAi() throws Exception {
        var server = startOpenAi();
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("request", "--llm", config, "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("Hello from fake openai!");

        // the request reached the fake server with model and prompt
        assertThat(server.requestBodies).hasSize(1);
        assertThat(server.requestBodies.get(0)).contains("gpt-4o-mini").contains("say hello");
    }

    @Test
    void requestOpenAiWithSystemPrompt() throws Exception {
        var server = startOpenAi();
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("request", "--llm", config, "--system", "You are a pirate.", "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(server.requestBodies.get(0)).contains("You are a pirate.").contains("system");
    }

    @Test
    void requestOllama() throws Exception {
        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("request", "--llm", config, "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("Hello from fake ollama!");
        assertThat(server.requestBodies.get(0)).contains("llama3.1").contains("say hello");
    }

    @Test
    void requestWithConfigFileAndInlineOverride() throws Exception {
        var server = startOpenAi();
        var file = java.nio.file.Files.createTempFile("llm", ".yaml");
        java.nio.file.Files.writeString(file, """
                provider: openai
                model: gpt-4o
                apiKey: test
                baseUrl: %s/v1
                """.formatted(server.baseUrl()));

        // inline config overrides the model only
        var r = run("request", "--llm-config", file.toString(), "--llm", "{model: gpt-4o-mini}", "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(server.requestBodies.get(0)).contains("gpt-4o-mini");
    }

    @Test
    void connectionFailure() throws Exception {
        // server on a closed port
        var config = "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:1', timeoutSeconds: 2}";
        var r = run("request", "--llm", config, "say hello");
        assertThat(r.exitCode()).isEqualTo(2);
        assertThat(r.out()).isEmpty();
    }

    @Test
    void missingConfigAndEmptyPrompt() throws Exception {
        assertThat(run("request", "say hello").exitCode()).isEqualTo(2);

        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";
        assertThat(run("request", "--llm", config, "").exitCode()).isEqualTo(2);
        assertThat(run("request", "--llm", "{invalid", "hi").exitCode()).isEqualTo(2);
    }
}
