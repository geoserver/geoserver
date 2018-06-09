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
 * @author Rob Atkinson, CSIRO
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class XYGeomTest extends AbstractAppSchemaTestSupport {

    @Override
    protected XYGeomMockData createTestData() {
        return new XYGeomMockData();
    }

    /** Test whether DescribeFeatureType returns xsd:schema. */
    @Test
    public void testDescribeFeatureType() {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=test:PointFeature");
        LOGGER.info("WFS DescribeFeatureType response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=test:PointFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=test:PointFeature");

        assertXpathCount(2, "//test:PointFeature", doc);
    }
}
