/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import org.geotools.data.complex.AppSchemaDataAccess;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test integration of {@link AppSchemaDataAccess} with GeoServer.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class XlinkWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected XlinkMockData createTestData() {
        return new XlinkMockData();
    }

    /** Test whether GetCapabilities returns wfs:WFS_Capabilities. */
    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wfs?request=GetCapabilities");
        LOGGER.info("WFS GetCapabilities response:\n" + prettyString(doc));
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
    }

    /** Test whether DescribeFeatureType returns xsd:schema. */
    @Test
    public void testDescribeFeatureType() {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() {

        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");

        LOGGER.info("WFS testGetFeatureContent response:\n" + prettyString(doc));

        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        assertXpathEvaluatesTo(
                "GUNTHORPE FORMATION", "//gsml:MappedFeature[@gml:id='mf1']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25699",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/@xlink:href",
                doc);

        // mf2
        assertXpathEvaluatesTo(
                "MERCIA MUDSTONE GROUP", "//gsml:MappedFeature[@gml:id='mf2']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25678",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/@xlink:href",
                doc);

        // mf3
        assertXpathEvaluatesTo(
                "CLIFTON FORMATION", "//gsml:MappedFeature[@gml:id='mf3']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);

        // mf4
        assertXpathEvaluatesTo(
                "MURRADUC BASALT", "//gsml:MappedFeature[@gml:id='mf4']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25682",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/@xlink:href",
                doc);
    }
}
