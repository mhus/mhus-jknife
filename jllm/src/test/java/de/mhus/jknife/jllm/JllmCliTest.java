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

    /** minimal fake server speaking enough openai/ollama protocol (non streaming and streaming) */
    static class FakeLlmServer {

        final HttpServer server;
        final List<String> requestBodies = new CopyOnWriteArrayList<>();
        final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();
        private final String nonStreamResponse;
        private final String[] streamChunks; // openai: sse 'data: {...}' lines, ollama: ndjson lines
        private final String streamContentType;

        FakeLlmServer(String nonStreamResponse, String streamContentType, String... streamChunks) throws IOException {
            this.nonStreamResponse = nonStreamResponse;
            this.streamContentType = streamContentType;
            this.streamChunks = streamChunks;
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requestBodies.add(body);
                authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] bytes;
                if (isStreamingRequest(body)) {
                    var buffer = new StringBuilder();
                    for (String chunk : streamChunks) {
                        buffer.append(chunk);
                    }
                    bytes = buffer.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", streamContentType);
                } else {
                    bytes = nonStreamResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                }
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        private static boolean isStreamingRequest(String body) {
            return body.contains("\"stream\" : true") || body.contains("\"stream\":true");
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

    private static final String OPENAI_STREAM_CHUNKS[] = {
            "data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hello \"},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"world\"},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":2,\"total_tokens\":7}}\n\n",
            "data: [DONE]\n\n" };

    private static final String OLLAMA_STREAM_CHUNKS[] = {
            "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"Hello \"},\"done\":false}\n",
            "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"world\"},\"done\":false}\n",
            "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true,\"done_reason\":\"stop\",\"prompt_eval_count\":5,\"eval_count\":2}\n" };

    private record Result(int exitCode, String out, String err) {
    }

    private final List<FakeLlmServer> servers = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(FakeLlmServer::stop);
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

    private FakeLlmServer startOpenAi() throws IOException {
        var server = new FakeLlmServer(OPENAI_RESPONSE, "text/event-stream", OPENAI_STREAM_CHUNKS);
        servers.add(server);
        return server;
    }

    private FakeLlmServer startOllama() throws IOException {
        var server = new FakeLlmServer(OLLAMA_RESPONSE, "application/x-ndjson", OLLAMA_STREAM_CHUNKS);
        servers.add(server);
        return server;
    }

    @Test
    void askOpenAi() throws Exception {
        var server = startOpenAi();
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("ask", "--llm", config, "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("Hello from fake openai!");

        // the request reached the fake server with model and prompt
        assertThat(server.requestBodies).hasSize(1);
        assertThat(server.requestBodies.get(0)).contains("gpt-4o-mini").contains("say hello");
    }

    @Test
    void askOpenAiWithSystemPrompt() throws Exception {
        var server = startOpenAi();
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("ask", "--llm", config, "--system", "You are a pirate.", "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(server.requestBodies.get(0)).contains("You are a pirate.").contains("system");
    }

    @Test
    void askOllama() throws Exception {
        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("ask", "--llm", config, "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("Hello from fake ollama!");
        assertThat(server.requestBodies.get(0)).contains("llama3.1").contains("say hello");
    }

    @Test
    void askWithConfigFileAndInlineOverride() throws Exception {
        var server = startOpenAi();
        var file = java.nio.file.Files.createTempFile("llm", ".yaml");
        java.nio.file.Files.writeString(file, """
                provider: openai
                model: gpt-4o
                apiKey: test
                baseUrl: %s/v1
                """.formatted(server.baseUrl()));

        // inline config overrides the model only
        var r = run("ask", "--llm-config", file.toString(), "--llm", "{model: gpt-4o-mini}", "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(server.requestBodies.get(0)).contains("gpt-4o-mini");
    }

    @Test
    void connectionFailure() throws Exception {
        // server on a closed port
        var config = "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:1', timeoutSeconds: 2}";
        var r = run("ask", "--llm", config, "say hello");
        assertThat(r.exitCode()).isEqualTo(2);
        assertThat(r.out()).isEmpty();
    }

    @Test
    void streamOpenAi() throws Exception {
        var server = startOpenAi();
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: '" + server.baseUrl() + "/v1'}";

        var r = run("stream", "--llm", config, "--perf", "say hello");
        assertThat(r.exitCode()).isZero();
        // streaming request was used
        assertThat(server.requestBodies.get(0)).contains("gpt-4o-mini").contains("true");
        // response text assembled from the chunks
        assertThat(r.out().trim()).isEqualTo("Hello world");
        // perf block on stderr
        assertThat(r.err()).contains("--- performance ---");
        assertThat(r.err()).contains("provider: openai");
        assertThat(r.err()).contains("latency: ");
        assertThat(r.err()).contains("ttft: ");
        assertThat(r.err()).contains("baseUrl: ");
    }

    @Test
    void streamOllama() throws Exception {
        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("stream", "--llm", config, "--perf", "say hello");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out().trim()).isEqualTo("Hello world");
        assertThat(r.err()).contains("provider: ollama");
        assertThat(r.err()).contains("ttft: ");
        // token usage from the final ollama chunk
        assertThat(r.err()).contains("tokens: in=5 out=2");
        assertThat(r.err()).contains("output tokens/sec: ");
        assertThat(r.err()).contains("finish reason: ");
    }

    @Test
    void streamErrorPath() throws Exception {
        var config = "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:1', timeoutSeconds: 2}";
        var r = run("stream", "--llm", config, "--perf", "say hello");
        assertThat(r.exitCode()).isEqualTo(2);
        assertThat(r.out()).isEmpty();
    }

    @Test
    void rawRequestOpenAiPassthrough() throws Exception {
        var server = startOpenAi();
        var config = "{provider: openai, model: gpt-4o-mini, apiKey: test-key, baseUrl: '" + server.baseUrl() + "/v1'}";
        var rawJson = "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"raw hello\"}]}";

        var r = run("request", "--llm", config, rawJson);
        assertThat(r.exitCode()).isZero();
        // body passed through exactly as given
        assertThat(server.requestBodies.get(0)).isEqualTo(rawJson);
        // bearer auth was sent
        assertThat(server.authorizationHeaders.get(0)).isEqualTo("Bearer test-key");
        // raw response passed through byte for byte (no newline added)
        assertThat(r.out()).isEqualTo(OPENAI_RESPONSE);
    }

    @Test
    void rawRequestOllamaStreamingPassthrough() throws Exception {
        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var r = run("request", "--llm", config, "{\"model\":\"llama3.1\",\"messages\":[],\"stream\":true}");
        assertThat(r.exitCode()).isZero();
        // stream:true -> the ndjson stream is passed through raw, chunk by chunk
        assertThat(r.out()).contains("\"done\":false").contains("\"done\":true");
    }

    @Test
    void rawRequestHttpError() throws Exception {
        var errorServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        errorServer.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            var bytes = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        errorServer.start();
        try {
            var config = "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:"
                    + errorServer.getAddress().getPort() + "'}";
            var r = run("request", "--llm", config, "{}");
            assertThat(r.exitCode()).isEqualTo(2);
            assertThat(r.err()).contains("HTTP 500").contains("boom");
            assertThat(r.out()).isEmpty();
        } finally {
            errorServer.stop(0);
        }
    }

    @Test
    void rawRequestFromStdinAndEmpty() throws Exception {
        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";

        var oldIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("{\"raw\":true}".getBytes(StandardCharsets.UTF_8)));
            var r = run("request", "--llm", config);
            assertThat(r.exitCode()).isZero();
            assertThat(server.requestBodies.get(0)).isEqualTo("{\"raw\":true}");
        } finally {
            System.setIn(oldIn);
        }

        // empty body
        assertThat(run("request", "--llm", config, "").exitCode()).isEqualTo(2);
    }

    @Test
    void missingConfigAndEmptyPrompt() throws Exception {
        assertThat(run("ask", "say hello").exitCode()).isEqualTo(2);

        var server = startOllama();
        var config = "{provider: ollama, model: llama3.1, baseUrl: '" + server.baseUrl() + "'}";
        assertThat(run("ask", "--llm", config, "").exitCode()).isEqualTo(2);
        assertThat(run("ask", "--llm", "{invalid", "hi").exitCode()).isEqualTo(2);
    }
}
