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
package de.mhus.jknife.jxpath;

import picocli.CommandLine;

/**
 * Not a test: run with the native-image-agent to record all reflection, resources and resource bundles used by the jdk
 * xpath engine, e.g.
 *
 * java -agentlib:native-image-agent=config-output-dir=... \ -cp <test-classpath>
 * de.mhus.jknife.jxpath.NativeImageAgentHarness
 */
public class NativeImageAgentHarness {

    private static final String XML = "<books xmlns:x=\"http://example.com/ns\">"
            + "<book id=\"1\"><title alpha='t1'>Alpha</title></book>" + "<book id=\"2\"><title>Beta</title></book>"
            + "<x:item>v1</x:item></books>";

    private static final String[] EXPRESSIONS = {
            // node set selections
            "//book", "//book/title", "//book/@id", "//book[@id='2']", "//book[1]", "//x:item", "//title/@alpha",
            "//book/title/text()",
            // functions: nodeset -> value
            "count(//book)", "string(//title)", "sum(//book/@id)", "concat(//book[1], '-', //book[2])",
            "contains(//title, 'A')", "starts-with(//title, 'A')", "ends-with(//title, 'a')", "string-length(//title)",
            "substring(//title, 1, 2)", "substring-before(//title, 'p')", "substring-after(//title, 'p')",
            "normalize-space(//title)", "translate(//title, 'A', 'B')", "name(//book)", "local-name(//book)",
            "namespace-uri(//x:item)", "number(//book/@id)", "boolean(//book)", "not(//book)", "true()", "false()",
            "floor(1.5)", "ceiling(1.5)", "round(1.5)", "position()", "last()", "lang('en')", "generate-id(//book)",
            "id('1')",
            // error paths (cover error resource bundles)
            "count(", "unknown-fn(//book)", };

    public static void main(String[] args) {
        for (String expression : EXPRESSIONS) {
            // select with and without namespace mapping, pretty and compact
            new CommandLine(new JXPathCmd()).execute("select", "--ns", "x=http://example.com/ns", expression, XML);
            new CommandLine(new JXPathCmd()).execute("select", "-p", expression, XML);
            new CommandLine(new JXPathCmd()).execute("exists", "-q", expression, XML);
        }
        // invalid xml (parser error path)
        new CommandLine(new JXPathCmd()).execute("select", "//a", "not xml");
        System.out.println("harness done");
    }
}
