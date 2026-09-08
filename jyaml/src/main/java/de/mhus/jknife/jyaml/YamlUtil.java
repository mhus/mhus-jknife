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
package de.mhus.jknife.jyaml;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * YAML helpers: safe parsing (snakeyaml 2.x safe by default) and conversion into the jackson tree model without pojo
 * binding (graalvm friendly).
 */
public final class YamlUtil {

    private YamlUtil() {
    }

    /**
     * Loads yaml into plain java objects (Map/List/String/Number/Boolean/null).
     *
     * @throws IllegalArgumentException
     *             on invalid yaml
     */
    public static Object read(String yaml) {
        try {
            return new Yaml().load(yaml);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid YAML: " + firstLine(e.getMessage()));
        }
    }

    /**
     * Converts the plain object tree into a jackson tree. Non-string map keys are converted with String.valueOf.
     */
    public static com.fasterxml.jackson.databind.JsonNode toJson(Object value) {
        if (value == null)
            return NullNode.getInstance();
        if (value instanceof Map<?, ?> map) {
            ObjectNode object = ObjectNode.class
                    .cast(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
            for (Map.Entry<?, ?> entry : map.entrySet())
                object.set(String.valueOf(entry.getKey()), toJson(entry.getValue()));
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayNode array = ArrayNode.class
                    .cast(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
            for (Object item : iterable)
                array.add(toJson(item));
            return array;
        }
        if (value instanceof String s)
            return TextNode.valueOf(s);
        if (value instanceof Integer i)
            return IntNode.valueOf(i);
        if (value instanceof Long l)
            return LongNode.valueOf(l);
        if (value instanceof Double d)
            return DoubleNode.valueOf(d);
        if (value instanceof Float f)
            return DoubleNode.valueOf(f.doubleValue());
        if (value instanceof Boolean b)
            return BooleanNode.valueOf(b);
        if (value instanceof Number n)
            return DoubleNode.valueOf(n.doubleValue());
        return TextNode.valueOf(String.valueOf(value));
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }
}
