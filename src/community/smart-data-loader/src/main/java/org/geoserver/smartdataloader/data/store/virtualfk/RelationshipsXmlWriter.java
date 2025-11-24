/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Utility class that serializes {@link Relationships} to the XML representation expected by the virtual foreign key
 * parser.
 */
public final class RelationshipsXmlWriter {

    private RelationshipsXmlWriter() {
        // utility class
    }

    /**
     * Serializes the provided relationships into the XML fragment stored in the connection parameters.
     *
     * @param relationships relationships to serialize (empty or {@code null} collections yield an empty string)
     * @return XML string or an empty string when no relationships are provided
     */
    public static String toXml(Relationships relationships) {
        if (relationships == null || relationships.getRelationships() == null) {
            return "";
        }
        int serializedCount = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("<relationships>");
        for (Relationship relationship : relationships.getRelationships()) {
            if (relationship == null) {
                continue;
            }
            serializedCount++;
            sb.append("<relationship");
            appendAttribute(sb, "name", relationship.getName());
            appendAttribute(sb, "cardinality", relationship.getCardinality());
            sb.append(">");
            appendEntityRef(sb, "source", relationship.getSource());
            appendEntityRef(sb, "target", relationship.getTarget());
            sb.append("</relationship>");
        }
        sb.append("</relationships>");
        return serializedCount == 0 ? "" : sb.toString();
    }

    private static void appendEntityRef(StringBuilder sb, String tagName, EntityRef ref) {
        if (ref == null) {
            return;
        }
        sb.append("<").append(tagName);
        appendAttribute(sb, "schema", ref.getSchema());
        appendAttribute(sb, "entity", ref.getEntity());
        appendAttribute(sb, "kind", ref.getKind());
        sb.append(">");
        if (ref.getKey() != null) {
            sb.append("<key");
            appendAttribute(sb, "column", ref.getKey().getColumn());
            sb.append("/>");
        }
        sb.append("</").append(tagName).append(">");
    }

    private static void appendAttribute(StringBuilder sb, String name, String value) {
        if (value == null) {
            return;
        }
        sb.append(" ").append(name).append("=\"").append(escape(value)).append("\"");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return StringEscapeUtils.escapeXml11(value);
    }
}
