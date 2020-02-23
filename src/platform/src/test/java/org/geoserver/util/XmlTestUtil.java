/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.util;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An aid for XML related testing.
 *
 * @author Kevin Smith, Boundless
 */
public class XmlTestUtil {
    private OutputStream showXML = null;
    private Map<String, String> namespaces = new HashMap<>();
    private org.custommonkey.xmlunit.NamespaceContext namespaceContext;

    private void regenerateContext() {
        namespaceContext = new SimpleNamespaceContext(namespaces);
    }

    public XmlTestUtil() {
        regenerateContext();
    }

    /** Set an output stream to print XML to when a matcher fails. Null to disable. */
    public void setShowXML(OutputStream showXML) {
        this.showXML = showXML;
    }

    /** Add a namespace to be used when resolving XPath expressions. */
    public void addNamespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
        regenerateContext();
    }

    /**
     * Match a document where one node matched the XPath expression, and it also matches the given
     * matcher.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Matcher<Document> hasOneNode(final String xPath, final Matcher<? super Node> matcher) {
        return hasNodes(xPath, (Matcher) contains(matcher));
    }
    /** Match a document where one node matches the XPath expression. */
    public Matcher<Document> hasOneNode(final String xPath) {
        return hasOneNode(xPath, any(Node.class));
    }
    /**
     * Match a document at least one of the nodes matched by the given XPath expression matches the
     * given matcher.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Matcher<Document> hasNode(final String xPath, final Matcher<Node> matcher) {
        return hasNodes(xPath, (Matcher) hasItem(matcher));
    }

    /** Match a document at least one node matches the given XPath. */
    public Matcher<Document> hasNode(final String xPath) {
        return hasNode(xPath, any(Node.class));
    }

    /**
     * Match a document where the list of nodes selected by the given XPath expression also matches
     * the given matcher.
     */
    public Matcher<Document> hasNodes(
            final String xPath, final Matcher<? extends Iterable<Node>> matcher) {
        return new BaseMatcher<Document>() {

            @Override
            public boolean matches(Object item) {
                XpathEngine engine = XMLUnit.newXpathEngine();
                engine.setNamespaceContext(namespaceContext);
                try {
                    List<Node> nodes =
                            nodeCollection(engine.getMatchingNodes(xPath, (Document) item));
                    return matcher.matches(nodes);
                } catch (XpathException e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("Document where the list of nodes matching ")
                        .appendValue(xPath)
                        .appendText(" is ")
                        .appendDescriptionOf(matcher);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                XpathEngine engine = XMLUnit.newXpathEngine();
                engine.setNamespaceContext(namespaceContext);
                try {
                    List<Node> nodes =
                            nodeCollection(engine.getMatchingNodes(xPath, (Document) item));

                    matcher.describeMismatch(nodes, description);

                    if (showXML != null) {
                        printDom((Document) item, showXML);
                    }
                } catch (XpathException e) {
                    description.appendText("exception occured: ").appendText(e.getMessage());
                }
            }
        };
    }

    /** Make a Java List out of a DOM NodeList. */
    public static List<Node> nodeCollection(final NodeList nl) {
        return new AbstractList<Node>() {

            @Override
            public Node get(int index) {
                return nl.item(index);
            }

            @Override
            public int size() {
                return nl.getLength();
            }
        };
    }

    /**
     * Print a DOM tree to an output stream or if there is an exception while doing so, print the
     * stack trace.
     */
    public static void printDom(Node dom, OutputStream os) {
        Transformer trans;
        PrintWriter w = new PrintWriter(os);
        try {
            TransformerFactory fact = TransformerFactory.newInstance();
            trans = fact.newTransformer();
            trans.transform(new DOMSource(dom), new StreamResult(new OutputStreamWriter(os)));
        } catch (TransformerException e) {
            w.println("An error ocurred while transforming the given DOM:");
            e.printStackTrace(w);
        }
    }
}
