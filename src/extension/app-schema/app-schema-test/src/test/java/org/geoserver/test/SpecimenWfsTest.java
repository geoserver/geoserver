/*
 * Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import org.junit.Test;

import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;

/**
 * WFS test based on samplingSpecimen 2.0, a GML 3.2 application schema.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class SpecimenWfsTest extends AbstractAppSchemaTestSupport {

    /**
     * @see org.geoserver.test.AbstractAppSchemaTestSupport#buildTestData()
     */
    @Override
    protected SpecimenMockData createTestData() {
        return new SpecimenMockData();
    }

    /**
     * Test whether GetFeature returns wfs:FeatureCollection.
     */
    @Test
    public void testGetFeature() {
        String path = "wfs?version=1.1.0&request=GetFeature&typename=spec:SF_Specimen&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertEquals(WFS.NAMESPACE, doc.getDocumentElement().getNamespaceURI());
        assertEquals("FeatureCollection", doc.getDocumentElement().getLocalName());
    }

    /**
     * Test whether GetFeature response is schema-valid.
     */
    @Test
    public void testGetFeatureValid() {
        String path = "wfs?version=1.1.0&request=GetFeature&typename=spec:SF_Specimen&outputformat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        validateGet(path);
    }

}
