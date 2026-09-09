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
 * (openai + ollama), e.g.
 *
 * java -agentlib:native-image-agent=config-output-dir=... \ -cp <test-classpath>
 * de.mhus.jknife.jllm.NativeImageAgentHarness
 */
public class NativeImageAgentHarness {

    public static void main(String[] args) throws Exception {
        var openAi = start("{\"id\":\"x\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"m\","
                + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}");
        try {
            new picocli.CommandLine(new JllmCmd()).execute("request", "--llm",
                    "{provider: openai, model: gpt-4o-mini, apiKey: test, baseUrl: 'http://localhost:"
                            + openAi.getAddress().getPort() + "/v1', temperature: 0.7, maxTokens: 100}",
                    "--system", "system prompt", "harness prompt");
        } finally {
            openAi.stop(0);
        }

        var ollama = start("{\"model\":\"llama3.1\",\"created_at\":\"2024-09-08T12:00:00Z\","
                + "\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"done\":true}");
        try {
            new picocli.CommandLine(new JllmCmd()).execute("request", "--llm",
                    "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:" + ollama.getAddress().getPort()
                            + "'}",
                    "harness prompt");
        } finally {
            ollama.stop(0);
        }

        // error path (connection refused)
        new picocli.CommandLine(new JllmCmd()).execute("request", "--llm",
                "{provider: ollama, model: llama3.1, baseUrl: 'http://localhost:1', timeoutSeconds: 1}",
                "harness prompt");

        System.out.println("harness done");
    }

    private static HttpServer start(String responseBody) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }
}
