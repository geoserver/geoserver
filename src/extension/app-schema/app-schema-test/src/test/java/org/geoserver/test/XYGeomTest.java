/*
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

import org.geotools.data.complex.AppSchemaDataAccess;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test integration of {@link AppSchemaDataAccess} with GeoServer.
 * 
 * @author Rob Atkinson, CSIRO
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class XYGeomTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new XYGeomTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new XYGeomMockData();
    }

    /**
     * Test whether DescribeFeatureType returns xsd:schema.
     */
    public void testDescribeFeatureType() {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=test:PointFeature");
        LOGGER.info("WFS DescribeFeatureType response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    /**
     * Test whether GetFeature returns wfs:FeatureCollection.
     */
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=test:PointFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }

    /**
     * Test content of GetFeature response.
     */
    public void testGetFeatureContent() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=test:PointFeature");

        assertXpathCount(2, "//test:PointFeature", doc);
    }

}
