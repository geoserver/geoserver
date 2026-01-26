/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses the virtual relationship XML fragments stored in the connection parameters into the in-memory
 * {@link Relationships} representation used server-side.
 */
public class RelationshipsXmlParser {

    static final int MAX_DOCUMENT_BYTES = 1_000_000;
    static final int MAX_DOCUMENT_DEPTH = 64;

    /**
     * Parses a relationships XML string and performs validation to ensure every relationship definition is complete.
     *
     * @param xml XML string containing {@code <relationships>} as root element. DTDs and external entities are
     *     disabled; documents larger than {@value MAX_DOCUMENT_BYTES} bytes or deeper than {@value MAX_DOCUMENT_DEPTH}
     *     elements are rejected.
     * @return relationships described in the document (never {@code null})
     * @throws Exception if the XML cannot be parsed or contains malformed definitions
     */
    public static Relationships parse(String xml) throws Exception {
        byte[] xmlBytes = xml == null ? new byte[0] : xml.getBytes(StandardCharsets.UTF_8);
        if (xmlBytes.length > MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException(
                    "Relationships XML exceeds maximum size of " + MAX_DOCUMENT_BYTES + " bytes");
        }

        DocumentBuilderFactory factory = buildSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        doc.getDocumentElement().normalize();
        enforceDepthLimit(doc);

        Relationships relationships = new Relationships();
        NodeList relNodes = doc.getElementsByTagName("relationship");
        for (int i = 0; i < relNodes.getLength(); i++) {
            Node relNode = relNodes.item(i);
            if (!(relNode instanceof Element)) {
                continue;
            }
            Relationship relationship = parseRelationship((Element) relNode, i);
            relationships.addRelationship(relationship);
        }
        return relationships;
    }

    private static DocumentBuilderFactory buildSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory;
    }

    private static void enforceDepthLimit(Document document) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }
        int depth = measureDepth(root, 1);
        if (depth > MAX_DOCUMENT_DEPTH) {
            throw new IllegalArgumentException(
                    "Relationships XML exceeds maximum depth of " + MAX_DOCUMENT_DEPTH + " elements");
        }
    }

    private static int measureDepth(Node node, int currentDepth) {
        int maxDepth = currentDepth;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                maxDepth = Math.max(maxDepth, measureDepth(child, currentDepth + 1));
            }
        }
        return maxDepth;
    }

    private static Relationship parseRelationship(Element relElem, int index) {
        if (relElem == null) {
            throw new IllegalArgumentException("Relationship definition #" + index + " is null");
        }
        String name = requireAttribute(relElem, "name", "relationship #" + index);
        String cardinality = requireAttribute(relElem, "cardinality", "relationship '" + name + "'");
        // Validate that the provided cardinality is supported.
        resolveCardinality(cardinality);

        Element sourceElement = requireChild(relElem, "source", "relationship '" + name + "'");
        Element targetElement = requireChild(relElem, "target", "relationship '" + name + "'");
        EntityRef source = parseEntityRef(sourceElement, "source", name);
        EntityRef target = parseEntityRef(targetElement, "target", name);
        return new Relationship(name, cardinality, source, target);
    }

    private static EntityRef parseEntityRef(Element elem, String role, String relationshipName) {
        if (elem == null) {
            throw new IllegalArgumentException(
                    String.format("Relationship '%s' is missing %s definition", relationshipName, role));
        }
        String context = String.format("%s of relationship '%s'", role, relationshipName);
        String schema = requireAttribute(elem, "schema", context);
        String entity = requireAttribute(elem, "entity", context);
        String kind = requireAttribute(elem, "kind", context);
        Element keyElem = requireChild(elem, "key", context);
        String column = requireAttribute(keyElem, "column", context + " key");
        Key key = new Key(column);
        return new EntityRef(schema, entity, kind, key);
    }

    private static Element requireChild(Element parent, String tagName, String context) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && node.getParentNode() == parent) {
                return (Element) node;
            }
        }
        throw new IllegalArgumentException(String.format("Missing <%s> for %s", tagName, context));
    }

    private static String requireAttribute(Element element, String attribute, String context) {
        String value = trimToNull(element.getAttribute(attribute));
        if (value == null) {
            throw new IllegalArgumentException(String.format("Attribute '%s' is required for %s", attribute, context));
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Resolves cardinality string to DomainRelationType enum. */
    public static DomainRelationType resolveCardinality(String cardinality) {
        switch (cardinality.toLowerCase()) {
            case "n:1":
                return DomainRelationType.MANYONE;
            case "1:n":
                return DomainRelationType.ONEMANY;
            case "1:1":
                return DomainRelationType.ONEONE;
            default:
                throw new IllegalArgumentException("Unknown cardinality: " + cardinality);
        }
    }
}
