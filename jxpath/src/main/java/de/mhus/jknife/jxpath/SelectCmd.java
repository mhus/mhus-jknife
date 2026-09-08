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

import de.mhus.jknife.shared.TextInput;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Evaluates an xpath and prints all matched nodes.
 */
@Command(name = "select", mixinStandardHelpOptions = true, description = "Evaluate an xpath expression and print all matched nodes (text/attribute values raw, elements as xml). Non nodeset expressions like count(...) or string(...) print their result. Exit code 0 if at least one node matched, 1 if none, 2 on error.")
public class SelectCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "XPATH", description = "The xpath expression, e.g. //book/title")
    private String xpathStr;

    @Parameters(index = "1", paramLabel = "XML", arity = "0..1", description = "The xml to process (default: read from stdin)")
    private String xml;

    @Mixin
    private TextInput input;

    @Option(names = { "-p", "--pretty" }, description = "Pretty print elements instead of compact")
    private boolean pretty;

    @Option(names = {
            "--ns" }, paramLabel = "PREFIX=URI", description = "Namespace mapping, repeatable, e.g. --ns x=http://example.com/ns")
    private List<String> namespaceList;

    @Override
    public Integer call() throws Exception {
        Map<String, String> namespaces;
        try {
            namespaces = parseNamespaces();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        var document = parse(xml);
        if (document == null)
            return 2;

        var xpath = XPathUtil.xpath(namespaces);
        try {
            // nodeset expressions
            var nodes = (NodeList) xpath.evaluate(xpathStr, document, XPathConstants.NODESET);
            int found = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                System.out.println(XPathUtil.serialize(nodes.item(i), pretty));
                found++;
            }
            input.verbose("Nodes: " + found);
            return found > 0 ? 0 : 1;
        } catch (javax.xml.xpath.XPathExpressionException e) {
            // not a nodeset expression: try string result (count(), string(), boolean(), ...)
            try {
                System.out.println(xpath.evaluate(xpathStr, document, XPathConstants.STRING));
                return 0;
            } catch (javax.xml.xpath.XPathExpressionException e2) {
                System.err.println("Invalid xpath: " + firstLine(e2.getMessage()));
                return 2;
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }

    private org.w3c.dom.Document parse(String xml) throws Exception {
        try {
            return XPathUtil.parseXml(input.resolveText(xml));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private Map<String, String> parseNamespaces() {
        var map = new LinkedHashMap<String, String>();
        if (namespaceList == null)
            return map;
        for (String ns : namespaceList) {
            int eq = ns.indexOf('=');
            if (eq <= 0)
                throw new IllegalArgumentException("Invalid namespace mapping (expected PREFIX=URI): '" + ns + "'");
            map.put(ns.substring(0, eq), ns.substring(eq + 1));
        }
        return map;
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }
}
