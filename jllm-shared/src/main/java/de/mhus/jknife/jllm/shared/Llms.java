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
package de.mhus.jknife.jllm.shared;

import de.mhus.jknife.jllm.shared.LlmConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

/**
 * Creates langchain4j chat models from a resolved {@link LlmConfig}.
 */
public final class Llms {

    private Llms() {
    }

    public static ChatModel chatModel(LlmConfig config) {
        return switch (config.provider()) {
        case LlmConfig.PROVIDER_OPENAI -> openAi(config);
        case LlmConfig.PROVIDER_OLLAMA -> ollama(config);
        default -> throw new IllegalArgumentException("Unknown llm provider: " + config.provider());
        };
    }

    private static ChatModel openAi(LlmConfig config) {
        var builder = OpenAiChatModel.builder().baseUrl(config.baseUrl()).apiKey(config.apiKey())
                .modelName(config.model()).timeout(Duration.ofSeconds(config.timeoutSeconds()));
        if (config.temperature() != null)
            builder.temperature(config.temperature());
        if (config.maxTokens() != null)
            builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    private static ChatModel ollama(LlmConfig config) {
        var builder = OllamaChatModel.builder().baseUrl(config.baseUrl()).modelName(config.model())
                .timeout(Duration.ofSeconds(config.timeoutSeconds()));
        if (config.temperature() != null)
            builder.temperature(config.temperature());
        return builder.build();
    }
}
