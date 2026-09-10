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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Not a test: run with the native-image-agent to record all reflection, proxies and resources used by langchain4j
 * (openai + ollama, non streaming and streaming/perf), e.g.
 *
 * java -agentlib:native-image-agent=config-output-dir=... \ -cp <test-classpath>
 * de.mhus.jknife.jllm.NativeImageAgentHarness
 */
public class NativeImageAgentHarness {

    private static final String OPENAI_NON_STREAM = "{\"id\":\"x\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"m\","
            + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}],"
            + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}";

    private static final String OLLAMA_NON_STREAM = "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\","
            + "\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"done\":true}";

    private static final String[] OPENAI_STREAM = {
            "data: {\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"m\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"o\"},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"m\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"k\"},\"finish_reason\":null}]}\n\n",
            "data: {\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"m\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}\n\n",
            "data: [DONE]\n\n" };

    private static final String[] OLLAMA_STREAM = {
            "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"o\"},\"done\":false}\n",
            "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"k\"},\"done\":false}\n",
            "{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true,\"done_reason\":\"stop\",\"prompt_eval_count\":1,\"eval_count\":1}\n" };

    public static void main(String[] args) throws Exception {
        // non streaming
        var openAi = start(OPENAI_NON_STREAM, "text/event-stream", OPENAI_STREAM);
        try {
            new picocli.CommandLine(new JllmCmd()).execute("ask", "--llm",
                    "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: 'http://localhost:"
                            + openAi.getAddress().getPort() + "/v1', temperature: 0.7, maxTokens: 100}",
                    "--system", "system prompt", "harness prompt");
            // streaming path (live tokens + perf)
            new picocli.CommandLine(new JllmCmd()).execute("stream", "--llm",
                    "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: 'http://localhost:"
                            + openAi.getAddress().getPort() + "/v1'}",
                    "--perf", "harness prompt");
            // raw request passthrough (jdk http client)
            new picocli.CommandLine(new JllmCmd()).execute("request", "--llm",
                    "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: 'http://localhost:"
                            + openAi.getAddress().getPort() + "/v1'}",
                    "{\"model\":\"gpt-4o-mini\",\"messages\":[],\"stream\":true}");
        } finally {
            openAi.stop(0);
        }

        var ollama = start(OLLAMA_NON_STREAM, "application/x-ndjson", OLLAMA_STREAM);
        try {
            new picocli.CommandLine(new JllmCmd()).execute("ask", "--llm",
                    "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:" + ollama.getAddress().getPort()
                            + "'}",
                    "harness prompt");
            new picocli.CommandLine(new JllmCmd()).execute("stream", "--llm",
                    "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:" + ollama.getAddress().getPort()
                            + "'}",
                    "--perf", "harness prompt");
            new picocli.CommandLine(new JllmCmd()).execute(
                    "request", "--llm", "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:"
                            + ollama.getAddress().getPort() + "'}",
                    "{\"model\":\"llama3.1\",\"messages\":[],\"stream\":true}");
        } finally {
            ollama.stop(0);
        }

        // error path (connection refused)
        new picocli.CommandLine(new JllmCmd()).execute("ask", "--llm",
                "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:1', timeoutSeconds: 1}",
                "harness prompt");

        System.out.println("harness done");
    }

    private static HttpServer start(String nonStreamResponse, String streamContentType, String[] streamChunks)
            throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes;
            String contentType;
            if (body.contains("\"stream\" : true") || body.contains("\"stream\":true")) {
                var buffer = new StringBuilder();
                for (String chunk : streamChunks)
                    buffer.append(chunk);
                bytes = buffer.toString().getBytes(StandardCharsets.UTF_8);
                contentType = streamContentType;
            } else {
                bytes = nonStreamResponse.getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }
}
