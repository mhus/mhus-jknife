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
package de.mhus.jknife.jjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON helpers: parse to the jackson tree model (no pojo binding, so it stays graalvm friendly) and simple path
 * navigation.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Parses json into the tree model.
     *
     * @throws IllegalArgumentException
     *             on invalid json
     */
    public static JsonNode read(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            if (node == null || node.isMissingNode())
                throw new IllegalArgumentException("Invalid JSON: empty input");
            return node;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + shortMessage(e));
        }
    }

    /**
     * Navigates to a node using a simple path: "a.b[0].c" or "$.a.b". Array indexes with [n], object keys with ['key']
     * or plain names.
     *
     * @return the node or null if the path does not exist
     *
     * @throws IllegalArgumentException
     *             for malformed paths
     */
    public static JsonNode navigate(JsonNode root, String path) {
        if (path == null || path.isBlank() || path.equals("$"))
            return root;
        if (path.startsWith("$"))
            path = path.substring(1);
        while (path.startsWith("."))
            path = path.substring(1);
        if (path.isBlank())
            return root;

        JsonNode node = root;
        for (String segment : path.split("\\.")) {
            int bracket = segment.indexOf('[');
            String name = bracket >= 0 ? segment.substring(0, bracket) : segment;
            String rest = bracket >= 0 ? segment.substring(bracket) : "";
            if (!name.isEmpty()) {
                node = node.get(name);
                if (node == null)
                    return null;
            }
            while (!rest.isEmpty()) {
                if (!rest.startsWith("["))
                    throw new IllegalArgumentException("Invalid path segment: '" + segment + "'");
                int end = rest.indexOf(']');
                if (end < 0)
                    throw new IllegalArgumentException("Invalid path segment: '" + segment + "'");
                String inside = rest.substring(1, end);
                if (inside.matches("\\d+")) {
                    node = node.get(Integer.parseInt(inside));
                } else if (inside.length() >= 2 && ((inside.startsWith("'") && inside.endsWith("'"))
                        || (inside.startsWith("\"") && inside.endsWith("\"")))) {
                    node = node.get(inside.substring(1, inside.length() - 1));
                } else {
                    throw new IllegalArgumentException("Invalid path segment: '" + segment + "'");
                }
                if (node == null)
                    return null;
                rest = rest.substring(end + 1);
            }
        }
        return node;
    }

    private static String shortMessage(Exception e) {
        var msg = e.getMessage();
        return msg == null ? e.toString() : msg.split("\n")[0];
    }
}
