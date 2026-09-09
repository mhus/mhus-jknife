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
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * Converts plain yaml objects (Map/List/String/Number/Boolean/null) into the jackson tree model without pojo binding
 * (graalvm friendly).
 */
public final class YamlToJson {

    private YamlToJson() {
    }

    /**
     * Parses yaml or json content (json is a subset of yaml) into a JsonNode.
     *
     * @throws IllegalArgumentException
     *             on invalid content
     */
    public static JsonNode parse(String yamlOrJson, String source) {
        Object value;
        try {
            value = new Yaml().load(yamlOrJson);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid llm config in " + source + ": " + firstLine(e.getMessage()));
        }
        return convert(value);
    }

    public static JsonNode convert(Object value) {
        if (value == null)
            return com.fasterxml.jackson.databind.node.NullNode.getInstance();
        if (value instanceof Map<?, ?> map)
            return mapNode(map);
        if (value instanceof Iterable<?> iterable) {
            var array = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            for (Object item : iterable)
                array.add(convert(item));
            return array;
        }
        if (value instanceof String s)
            return com.fasterxml.jackson.databind.node.TextNode.valueOf(s);
        if (value instanceof Integer i)
            return com.fasterxml.jackson.databind.node.IntNode.valueOf(i);
        if (value instanceof Long l)
            return com.fasterxml.jackson.databind.node.LongNode.valueOf(l);
        if (value instanceof Double d)
            return com.fasterxml.jackson.databind.node.DoubleNode.valueOf(d);
        if (value instanceof Float f)
            return com.fasterxml.jackson.databind.node.DoubleNode.valueOf(f.doubleValue());
        if (value instanceof Boolean b)
            return com.fasterxml.jackson.databind.node.BooleanNode.valueOf(b);
        if (value instanceof Number n)
            return com.fasterxml.jackson.databind.node.DoubleNode.valueOf(n.doubleValue());
        return com.fasterxml.jackson.databind.node.TextNode.valueOf(String.valueOf(value));
    }

    private static JsonNode mapNode(Map<?, ?> map) {
        var object = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        for (Map.Entry<?, ?> entry : map.entrySet())
            object.set(String.valueOf(entry.getKey()), convert(entry.getValue()));
        return object;
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }
}
