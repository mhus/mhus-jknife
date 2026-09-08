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
 * Tests if an xpath matches at least one node.
 */
@Command(name = "exists", mixinStandardHelpOptions = true, description = "Test if an xpath matches at least one node. Exit code 0 if matching, 1 if not, 2 on error.")
public class ExistsCmd implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "XPATH", description = "The xpath expression")
    private String xpathStr;

    @Parameters(index = "1", paramLabel = "XML", arity = "0..1", description = "The xml to process (default: read from stdin)")
    private String xml;

    @Mixin
    private TextInput input;

    @Option(names = { "-q", "--quiet" }, description = "Do not print the result, only set the exit code")
    private boolean quiet;

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

        try {
            var nodes = (NodeList) XPathUtil.xpath(namespaces).evaluate(xpathStr, document, XPathConstants.NODESET);
            boolean exists = nodes.getLength() > 0;
            input.verbose("Nodes: " + nodes.getLength());
            if (!quiet)
                System.out.println(exists);
            return exists ? 0 : 1;
        } catch (javax.xml.xpath.XPathExpressionException e) {
            System.err.println("Invalid xpath: " + e.getMessage().split("\n")[0]);
            return 2;
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
}
