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
import de.mhus.jknife.jllm.model.Llms;
import de.mhus.jknife.shared.TextInput;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Simple text in, text out: sends a prompt to an llm and prints the response.
 */
@Command(name = "request", mixinStandardHelpOptions = true, description = "Send a prompt to an llm and print the response (text in, text out). Exit code 0 on success, 2 on error.")
public class RequestCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "PROMPT", arity = "0..1", description = "The prompt to send (default: read from stdin)")
    private String prompt;

    @Mixin
    private TextInput input;

    @Option(names = { "--system" }, paramLabel = "TEXT", description = "System prompt / instruction for the model")
    private String system;

    @Option(names = {
            "--llm" }, paramLabel = "CONFIG", description = "Inline llm config, yaml or json, e.g. --llm '{provider: openai, model: gpt-4o-mini}'. Merged over --llm-config.")
    private String llmInline;

    @Option(names = { "--llm-config" }, paramLabel = "FILE", description = "Llm config file (yaml or json)")
    private String llmConfigFile;

    @Option(names = {
            "--perf" }, description = "Measure performance (uses streaming): ttft, latency, tokens/sec, token usage. The block is printed to stderr, the response to stdout.")
    private boolean perf;

    @Override
    public Integer call() throws Exception {
        LlmConfig config;
        try {
            config = LlmConfigLoader.load(llmInline, llmConfigFile);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        String prompt = input.resolveText(this.prompt);
        if (prompt == null || prompt.isBlank()) {
            System.err.println("Empty prompt");
            return 2;
        }

        input.verbose(
                "Config: provider=" + config.provider() + " model=" + config.model() + " baseUrl=" + config.baseUrl());

        try {
            var requestBuilder = dev.langchain4j.model.chat.request.ChatRequest.builder();
            if (system != null && !system.isBlank())
                requestBuilder.messages(SystemMessage.from(system), UserMessage.from(prompt));
            else
                requestBuilder.messages(UserMessage.from(prompt));

            if (perf)
                return runPerf(config, requestBuilder.build());

            ChatResponse response = Llms.chatModel(config).chat(requestBuilder.build());
            var aiMessage = response.aiMessage();
            String text = aiMessage == null ? null : aiMessage.text();
            if (text == null || text.isBlank()) {
                System.err.println("Empty response from model " + config.model());
                return 2;
            }
            verboseMetadata(response);
            System.out.println(text);
            return 0;
        } catch (RuntimeException e) {
            System.err.println("Llm request failed: " + firstLine(e.getMessage()));
            input.verbose(e.toString());
            return 2;
        }
    }

    private void verboseMetadata(ChatResponse response) {
        var metadata = response == null ? null : response.metadata();
        if (metadata == null)
            return;
        var usage = metadata.tokenUsage();
        if (usage != null)
            input.verbose("Tokens: in=" + usage.inputTokenCount() + " out=" + usage.outputTokenCount() + " total="
                    + usage.totalTokenCount());
        if (metadata.finishReason() != null)
            input.verbose("Finish reason: " + metadata.finishReason());
        if (metadata.modelName() != null)
            input.verbose("Response model: " + metadata.modelName());
    }

    private Integer runPerf(LlmConfig config, dev.langchain4j.model.chat.request.ChatRequest request)
            throws InterruptedException {
        var runner = new StreamingRunner();
        try {
            Llms.streamingChatModel(config).chat(request, runner);
        } catch (RuntimeException e) {
            System.err.println("Llm request failed: " + firstLine(e.getMessage()));
            return 2;
        }

        var result = runner.await(config.timeoutSeconds());
        if (result.error() != null) {
            System.err.println("Llm request failed: " + firstLine(result.error().getMessage()));
            input.verbose(result.error().toString());
            return 2;
        }

        if (result.text() == null || result.text().isBlank()) {
            System.err.println("Empty response from model " + config.model());
            return 2;
        }
        System.out.println(result.text());
        printPerfBlock(config, result);
        return 0;
    }

    private void printPerfBlock(LlmConfig config, StreamingRunner.Result result) {
        var err = System.err;
        err.println("--- performance ---");
        err.println("provider: " + config.provider());
        err.println("model: " + config.model());
        err.println("baseUrl: " + config.baseUrl());
        err.println("params: temperature=" + config.temperature() + " maxTokens=" + config.maxTokens() + " timeout="
                + config.timeoutSeconds() + "s");

        double totalSeconds = result.totalNanos() / 1_000_000_000.0;
        err.println("latency: " + String.format("%.3fs", totalSeconds));
        if (result.ttftNanos() >= 0)
            err.println("ttft: " + String.format("%.3fs", result.ttftNanos() / 1_000_000_000.0));
        else
            err.println("ttft: n/a");

        double generationSeconds = Math.max(0, result.totalNanos() - Math.max(0, result.ttftNanos())) / 1_000_000_000.0;

        var metadata = result.response() == null ? null : result.response().metadata();
        var usage = metadata == null ? null : metadata.tokenUsage();
        if (usage != null) {
            err.println("tokens: in=" + usage.inputTokenCount() + " out=" + usage.outputTokenCount() + " total="
                    + usage.totalTokenCount());
            if (usage.outputTokenCount() != null && usage.outputTokenCount() > 0 && generationSeconds > 0)
                err.println(
                        "output tokens/sec: " + String.format("%.1f", usage.outputTokenCount() / generationSeconds));
        }
        if (metadata != null) {
            if (metadata.finishReason() != null)
                err.println("finish reason: " + metadata.finishReason());
            if (metadata.modelName() != null && !metadata.modelName().isBlank())
                err.println("response model: " + metadata.modelName());
            if (metadata.id() != null)
                err.println("response id: " + metadata.id());
        }
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }
}
