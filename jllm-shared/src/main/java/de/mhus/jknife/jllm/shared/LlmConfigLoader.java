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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the llm configuration from an inline value (--llm, yaml or json) and/or a config file (--llm-config). The
 * inline value is merged over the file content, so a file can define the base config and the inline value can override
 * single fields (e.g. just the model).
 */
public final class LlmConfigLoader {

    private static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";

    private LlmConfigLoader() {
    }

    /**
     * @param inline
     *            optional inline yaml/json config, merged over the file config
     * @param file
     *            optional config file (yaml or json)
     *
     * @throws IllegalArgumentException
     *             for missing/invalid configuration
     * @throws IOException
     *             if the config file cannot be read
     */
    public static LlmConfig load(String inline, String file) throws IOException {
        JsonNode base = null;
        if (file != null && !file.isBlank()) {
            String content = Files.readString(Path.of(file));
            base = YamlToJson.parse(content, file);
        }
        JsonNode overlay = null;
        if (inline != null && !inline.isBlank())
            overlay = YamlToJson.parse(inline, "--llm");

        JsonNode merged = merge(base, overlay);
        if (merged == null || merged.isNull() || merged.isMissingNode()
                || (merged.isObject() && merged.isEmpty() && !merged.fields().hasNext()))
            throw new IllegalArgumentException(
                    "No llm config given: use --llm '<yaml or json>' and/or --llm-config <file> (fields: provider, model, baseUrl, apiKey, temperature, maxTokens, timeoutSeconds)");

        if (!merged.isObject())
            throw new IllegalArgumentException("The llm config must be a map, not " + nodeType(merged));

        // provider
        String provider = text(merged, "provider");
        if (provider == null)
            throw new IllegalArgumentException("Missing 'provider' in llm config (allowed: openai, ollama)");
        provider = provider.trim().toLowerCase();
        if (!LlmConfig.PROVIDER_OPENAI.equals(provider) && !LlmConfig.PROVIDER_OLLAMA.equals(provider))
            throw new IllegalArgumentException("Unknown llm provider '" + provider + "' (allowed: openai, ollama)");

        // model
        String model = text(merged, "model");
        if (model == null)
            throw new IllegalArgumentException(
                    "Missing 'model' in llm config (e.g. gpt-4o-mini for openai, llama3.1 for ollama)");

        // baseUrl with provider default
        String baseUrl = text(merged, "baseUrl");
        if (baseUrl == null)
            baseUrl = LlmConfig.PROVIDER_OPENAI.equals(provider) ? LlmConfig.DEFAULT_OPENAI_BASE_URL
                    : LlmConfig.DEFAULT_OLLAMA_BASE_URL;

        // apiKey: config or, for openai, environment fallback
        String apiKey = text(merged, "apiKey");
        if (apiKey == null && LlmConfig.PROVIDER_OPENAI.equals(provider))
            apiKey = System.getenv(OPENAI_API_KEY_ENV);
        if (apiKey == null && LlmConfig.PROVIDER_OPENAI.equals(provider))
            throw new IllegalArgumentException("Missing openai 'apiKey' in llm config (or set the " + OPENAI_API_KEY_ENV
                    + " environment variable)");

        Double temperature = doubleValue(merged, "temperature");
        Integer maxTokens = intValue(merged, "maxTokens");
        Long timeoutSeconds = longValue(merged, "timeoutSeconds");
        if (timeoutSeconds == null)
            timeoutSeconds = 60L;

        return new LlmConfig(provider, model, baseUrl, apiKey, temperature, maxTokens, timeoutSeconds);
    }

    /** deep merge: overlay values win, objects are merged recursively */
    static JsonNode merge(JsonNode base, JsonNode overlay) {
        if (base == null)
            return overlay;
        if (overlay == null)
            return base;
        if (base.isObject() && overlay.isObject()) {
            ObjectNode result = (ObjectNode) base.deepCopy();
            overlay.fieldNames()
                    .forEachRemaining(field -> result.set(field, merge(base.get(field), overlay.get(field))));
            return result;
        }
        return overlay;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull())
            return null;
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private static Double doubleValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private static Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private static String nodeType(JsonNode node) {
        return node.isArray() ? "an array" : "a " + node.getNodeType().toString().toLowerCase();
    }
}
