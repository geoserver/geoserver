/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS test based on samplingSpecimen 2.0, a GML 3.2 application schema.
 *
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class SpecimenWfsTest extends AbstractAppSchemaTestSupport {

    /** @see org.geoserver.test.AbstractAppSchemaTestSupport#buildTestData() */
    @Override
    protected SpecimenMockData createTestData() {
        return new SpecimenMockData();
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        String path =
                "wfs?version=1.1.0&request=GetFeature&typename=spec:SF_Specimen&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertEquals(WFS.NAMESPACE, doc.getDocumentElement().getNamespaceURI());
        assertEquals("FeatureCollection", doc.getDocumentElement().getLocalName());
    }

    /** Test whether GetFeature response is schema-valid. */
    @Test
    public void testGetFeatureValid() {
        String path =
                "wfs?version=1.1.0&request=GetFeature&typename=spec:SF_Specimen&outputformat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        validateGet(path);
    }

    /** Test whether GetFeature response has expected content. */
    @Test
    public void testGetFeatureContent() {
        String path =
                "wfs?version=1.1.0&request=GetFeature&typename=spec:SF_Specimen&outputformat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathEvaluatesTo("unknown", "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathCount(2, "//spec:SF_Specimen", doc);
        assertXpathEvaluatesTo("First", "//spec:SF_Specimen[@gml:id='specimen.1']/gml:name", doc);
        assertXpathEvaluatesTo("2.7", "//spec:SF_Specimen[@gml:id='specimen.1']/spec:size", doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/def/uom/UCUM/g",
                "//spec:SF_Specimen[@gml:id='specimen.1']/spec:size/@uom",
                doc);
        assertXpathEvaluatesTo("Second", "//spec:SF_Specimen[@gml:id='specimen.2']/gml:name", doc);
        assertXpathEvaluatesTo("0.31", "//spec:SF_Specimen[@gml:id='specimen.2']/spec:size", doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/def/uom/UCUM/g",
                "//spec:SF_Specimen[@gml:id='specimen.2']/spec:size/@uom",
                doc);
    }
}
