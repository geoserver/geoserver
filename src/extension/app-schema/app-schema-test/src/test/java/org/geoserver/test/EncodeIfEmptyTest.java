/*
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

import org.w3c.dom.Document;

/**
 * WFS test based for testing encodeIfEmpty tag
 * 
 * @author Victor Tey (CSIRO Earth Science and Resource Engineering)
 */
public class EncodeIfEmptyTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new EncodeIfEmptyTest());
    }

    /**
     * @see org.geoserver.test.AbstractAppSchemaWfsTestSupport#buildTestData()
     */
    @Override
    protected NamespaceTestData buildTestData() {
        return new EncodeIfEmptyMockData();
    }

    /**
     * Test whether GetFeature returns wfs:FeatureCollection.
     */
    public void testEncodeIfEmpty() {
        String path = "wfs?request=GetFeature&typename=om:OM_Observation&outputFormat=gml32";
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :\n" + prettyString(doc));

        assertXpathCount(
                1,
                "//om:OM_Observation[@gml:id='ID1']/om:result/wml2dr:MeasurementTimeseriesDomainRange[@gml:id='measurement.ID1']/gmlcov:rangeType/swe:DataRecord[@id='ID1']/swe:field/swe:Quantity/@definition",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/def/property/OGC/0/SamplingTime",
                "//om:OM_Observation[@gml:id='ID1']/om:result/wml2dr:MeasurementTimeseriesDomainRange[@gml:id='measurement.ID1']/gmlcov:rangeType/swe:DataRecord[@id='ID1']/swe:field/swe:Quantity/@definition",
                doc);
        assertXpathCount(
                0,
                "//om:OM_Observation[@gml:id='ID1']/om:result/wml2dr:MeasurementTimeseriesDomainRange[@gml:id='measurement.ID1']/gmlcov:rangeType/swe:DataRecord[@id='ID1']/swe:field/swe:Quantity/uom/@code",
                doc);
    }
}
