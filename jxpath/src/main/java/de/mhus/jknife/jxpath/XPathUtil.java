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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * XPath helpers: safe xml parsing (doctype declarations disabled), namespace aware xpath with optional prefix mappings
 * and node serialization.
 */
public final class XPathUtil {

    private XPathUtil() {
    }

    /**
     * Parses xml, namespace aware, doctype declarations disabled (security).
     *
     * @throws IllegalArgumentException
     *             on invalid xml
     */
    public static Document parseXml(String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            tryDisableDoctype(factory);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML: " + firstLine(e.getMessage()));
        }
    }

    private static void tryDisableDoctype(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
            // parser does not support the feature, continue
        }
    }

    /**
     * Creates an xpath with optional prefix=uri namespace mappings.
     */
    public static XPath xpath(Map<String, String> namespaces) {
        var xpath = XPathFactory.newInstance().newXPath();
        if (namespaces != null && !namespaces.isEmpty())
            xpath.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        return xpath;
    }

    /**
     * Serializes a node: attribute/text/cdata nodes print their value, everything else prints as xml.
     */
    public static String serialize(Node node, boolean pretty) throws Exception {
        switch (node.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
        case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
        case Node.COMMENT_NODE:
            var value = node.getNodeValue();
            return value == null ? "" : value;
        default:
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            if (pretty) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            var writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString().trim();
        }
    }

    private static String firstLine(String message) {
        return message == null ? "unknown error" : message.split("\n")[0];
    }

    private static final class SimpleNamespaceContext implements NamespaceContext {

        private final Map<String, String> namespaces;

        SimpleNamespaceContext(Map<String, String> namespaces) {
            this.namespaces = Map.copyOf(namespaces);
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null)
                throw new IllegalArgumentException("Null prefix");
            if (XMLConstants.XML_NS_PREFIX.equals(prefix))
                return XMLConstants.XML_NS_URI;
            if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            return namespaces.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            var i = getPrefixes(namespaceURI);
            return i.hasNext() ? i.next() : null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            if (namespaceURI == null)
                throw new IllegalArgumentException("Null namespace uri");
            for (var entry : namespaces.entrySet()) {
                if (entry.getValue().equals(namespaceURI))
                    return Collections.singletonList(entry.getKey()).iterator();
            }
            return Collections.<String> emptyIterator();
        }
    }
}
