/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class RelationshipsXmlParserTest {

    @Test
    public void testParseValidXml() throws Exception {
        String xml = "<relationships>" + "<relationship name=\"observations_has_station\" cardinality=\"n:1\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</target>"
                + "</relationship>"
                + "<relationship name=\"station_parameters\" cardinality=\"1:n\">"
                + "<source schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"parameters_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</target>"
                + "</relationship>"
                + "</relationships>";
        Relationships relationships = RelationshipsXmlParser.parse(xml);
        assertNotNull(relationships);
        assertEquals(2, relationships.getRelationships().size());
        Relationship r1 = relationships.getRelationships().get(0);
        assertEquals("observations_has_station", r1.getName());
        assertEquals("n:1", r1.getCardinality());
        assertEquals("public", r1.getSource().getSchema());
        assertEquals("observations_v", r1.getSource().getEntity());
        assertEquals("VIEW", r1.getSource().getKind());
        assertEquals("station_id", r1.getSource().getKey().getColumn());
        assertEquals("public", r1.getTarget().getSchema());
        assertEquals("stations", r1.getTarget().getEntity());
        assertEquals("TABLE", r1.getTarget().getKind());
        assertEquals("id", r1.getTarget().getKey().getColumn());
    }

    @Test
    public void testParseEmptyRelationships() throws Exception {
        String xml = "<relationships></relationships>";
        Relationships relationships = RelationshipsXmlParser.parse(xml);
        assertNotNull(relationships);
        assertEquals(0, relationships.getRelationships().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMissingRelationshipNameFails() throws Exception {
        String xml = "<relationships>"
                + "<relationship cardinality=\"n:1\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</target>"
                + "</relationship>"
                + "</relationships>";
        RelationshipsXmlParser.parse(xml);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMissingKeyColumnFails() throws Exception {
        String xml = "<relationships>"
                + "<relationship name=\"observations_has_station\" cardinality=\"n:1\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</target>"
                + "</relationship>"
                + "</relationships>";
        RelationshipsXmlParser.parse(xml);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidCardinalityFails() throws Exception {
        String xml = "<relationships>"
                + "<relationship name=\"observations_has_station\" cardinality=\"invalid\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</target>"
                + "</relationship>"
                + "</relationships>";
        RelationshipsXmlParser.parse(xml);
    }

    @Test(expected = Exception.class)
    public void testParseInvalidXml() throws Exception {
        String xml = "<relationships><relationship></relationships"; // malformed
        RelationshipsXmlParser.parse(xml);
    }

    @Test
    public void testRejectsExternalEntities() {
        String xml = "<!DOCTYPE relationships ["
                + "<!ENTITY xxe SYSTEM \"file:///etc/passwd\">"
                + "]>"
                + "<relationships>"
                + "<relationship name=\"evil\" cardinality=\"1:1\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"&xxe;\"/>"
                + "</target>"
                + "</relationship>"
                + "</relationships>";

        assertThrows(Exception.class, () -> RelationshipsXmlParser.parse(xml));
    }

    @Test
    public void testRejectsOversizedDocument() {
        int limit = RelationshipsXmlParser.MAX_DOCUMENT_BYTES;
        StringBuilder xml = new StringBuilder(limit + 100);
        xml.append("<relationships>");
        String relationship = "<relationship name=\"r\" cardinality=\"1:1\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</target>"
                + "</relationship>";
        while (xml.length() <= limit + 1) {
            xml.append(relationship);
        }
        xml.append("</relationships>");

        assertThrows(IllegalArgumentException.class, () -> RelationshipsXmlParser.parse(xml.toString()));
    }

    @Test
    public void testRejectsOverlyDeepDocument() {
        int depthLimit = RelationshipsXmlParser.MAX_DOCUMENT_DEPTH;
        StringBuilder xml = new StringBuilder();
        xml.append("<relationships>");
        for (int i = 0; i <= depthLimit; i++) {
            xml.append("<wrapper>");
        }
        xml.append("<relationship name=\"deep\" cardinality=\"1:1\">"
                + "<source schema=\"public\" entity=\"observations_v\" kind=\"VIEW\">"
                + "<key column=\"station_id\"/>"
                + "</source>"
                + "<target schema=\"public\" entity=\"stations\" kind=\"TABLE\">"
                + "<key column=\"id\"/>"
                + "</target>"
                + "</relationship>");
        for (int i = 0; i <= depthLimit; i++) {
            xml.append("</wrapper>");
        }
        xml.append("</relationships>");

        assertThrows(IllegalArgumentException.class, () -> RelationshipsXmlParser.parse(xml.toString()));
    }
}
