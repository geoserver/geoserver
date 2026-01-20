/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RelationshipsXmlWriterTest {

    @Test
    public void returnsEmptyStringWhenNoRelationships() {
        Relationships relationships = new Relationships();

        String xml = RelationshipsXmlWriter.toXml(relationships);

        assertEquals("", xml);
    }

    @Test
    public void returnsEmptyStringWhenAllRelationshipsNull() {
        Relationships relationships = new Relationships();
        relationships.addRelationship(null);

        String xml = RelationshipsXmlWriter.toXml(relationships);

        assertEquals("", xml);
    }

    @Test
    public void serializesSingleRelationship() {
        EntityRef source = new EntityRef("public", "observations_v", "VIEW", new Key("station_id"));
        EntityRef target = new EntityRef("public", "stations", "TABLE", new Key("id"));
        Relationship relationship = new Relationship("observations_has_station", "n:1", source, target);
        Relationships relationships = new Relationships();
        relationships.addRelationship(relationship);

        String xml = RelationshipsXmlWriter.toXml(relationships);

        assertTrue(xml.contains("<relationship"));
        assertTrue(xml.contains("name=\"observations_has_station\""));
        assertTrue(xml.contains("cardinality=\"n:1\""));
        assertTrue(xml.contains("<source"));
        assertTrue(xml.contains("<target"));
    }
}
