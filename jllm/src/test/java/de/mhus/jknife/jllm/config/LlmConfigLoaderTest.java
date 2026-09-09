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
package de.mhus.jknife.jllm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void inlineJson() throws Exception {
        var config = LlmConfigLoader.load(
                "{\"provider\":\"openai\",\"model\":\"gpt-4o-mini\",\"apiKey\":\"k\",\"temperature\":0.7,\"maxTokens\":100,\"timeoutSeconds\":30}",
                null);
        assertThat(config.provider()).isEqualTo("openai");
        assertThat(config.model()).isEqualTo("gpt-4o-mini");
        assertThat(config.baseUrl()).isEqualTo(LlmConfig.DEFAULT_OPENAI_BASE_URL);
        assertThat(config.apiKey()).isEqualTo("k");
        assertThat(config.temperature()).isEqualTo(0.7);
        assertThat(config.maxTokens()).isEqualTo(100);
        assertThat(config.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void inlineYaml() throws Exception {
        var config = LlmConfigLoader.load("{provider: ollama, model: llama3.1, baseUrl: 'http://x:11434'}", null);
        assertThat(config.provider()).isEqualTo("ollama");
        assertThat(config.model()).isEqualTo("llama3.1");
        assertThat(config.baseUrl()).isEqualTo("http://x:11434");
        assertThat(config.timeoutSeconds()).isEqualTo(60); // default
    }

    @Test
    void configFileYaml() throws Exception {
        var file = tempDir.resolve("llm.yaml");
        Files.writeString(file, """
                provider: ollama
                model: llama3.1
                temperature: 0.2
                """);
        var config = LlmConfigLoader.load(null, file.toString());
        assertThat(config.provider()).isEqualTo("ollama");
        assertThat(config.model()).isEqualTo("llama3.1");
        assertThat(config.temperature()).isEqualTo(0.2);
        assertThat(config.baseUrl()).isEqualTo(LlmConfig.DEFAULT_OLLAMA_BASE_URL);
    }

    @Test
    void inlineMergesOverFile() throws Exception {
        var file = tempDir.resolve("llm.yaml");
        Files.writeString(file, """
                provider: openai
                model: gpt-4o
                apiKey: file-key
                temperature: 0.5
                """);
        // override only the model, the rest comes from the file
        var config = LlmConfigLoader.load("{model: gpt-4o-mini}", file.toString());
        assertThat(config.model()).isEqualTo("gpt-4o-mini");
        assertThat(config.apiKey()).isEqualTo("file-key");
        assertThat(config.temperature()).isEqualTo(0.5);
    }

    @Test
    void noConfig() {
        assertThatThrownBy(() -> LlmConfigLoader.load(null, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No llm config given");
        assertThatThrownBy(() -> LlmConfigLoader.load("", null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No llm config given");
    }

    @Test
    void invalidContent() {
        assertThatThrownBy(() -> LlmConfigLoader.load("{provider: openai", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid llm config");
        assertThatThrownBy(() -> LlmConfigLoader.load("just text", null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a map");
    }

    @Test
    void missingProviderAndModel() {
        assertThatThrownBy(() -> LlmConfigLoader.load("{model: x}", null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider");
        assertThatThrownBy(() -> LlmConfigLoader.load("{provider: openai, apiKey: k}", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("model");
    }

    @Test
    void unknownProvider() {
        assertThatThrownBy(() -> LlmConfigLoader.load("{provider: anthropic, model: x}", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("anthropic");
    }

    @Test
    void openaiRequiresApiKey() {
        // no apiKey in config and OPENAI_API_KEY normally not set in test env
        var envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank())
            return; // cannot test reliably with a set env var
        assertThatThrownBy(() -> LlmConfigLoader.load("{provider: openai, model: gpt-4o-mini}", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("apiKey");
    }
}
