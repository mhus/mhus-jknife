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

/**
 * Resolved llm configuration. All fields optional except provider and model, defaults are applied by the
 * {@link LlmConfigLoader}.
 */
public record LlmConfig(String provider, String model, String baseUrl, String apiKey, Double temperature,
        Integer maxTokens, Long timeoutSeconds) {

    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_OLLAMA = "ollama";

    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
}
