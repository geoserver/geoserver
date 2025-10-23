/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;
import org.w3c.dom.*;

public class RelationshipsXmlParser {

    public static Relationships parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        Relationships relationships = new Relationships();
        NodeList relNodes = doc.getElementsByTagName("relationship");
        for (int i = 0; i < relNodes.getLength(); i++) {
            Element relElem = (Element) relNodes.item(i);
            String name = relElem.getAttribute("name");
            String cardinality = relElem.getAttribute("cardinality");

            EntityRef source = parseEntityRef(
                    (Element) relElem.getElementsByTagName("source").item(0));
            EntityRef target = parseEntityRef(
                    (Element) relElem.getElementsByTagName("target").item(0));

            Relationship relationship = new Relationship(name, cardinality, source, target);
            relationships.addRelationship(relationship);
        }
        return relationships;
    }

    private static EntityRef parseEntityRef(Element elem) {
        String schema = elem.getAttribute("schema");
        String entity = elem.getAttribute("entity");
        String kind = elem.getAttribute("kind");
        Element keyElem = (Element) elem.getElementsByTagName("key").item(0);
        String column = keyElem.getAttribute("column");
        Key key = new Key(column);
        return new EntityRef(schema, entity, kind, key);
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
            case "n:n":
                return DomainRelationType.MANYMANY;
            default:
                throw new IllegalArgumentException("Unknown cardinality: " + cardinality);
        }
    }
}
