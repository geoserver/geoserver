/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetFeatureBboxTest extends WFSTestSupport {

    @Test
    public void testFeatureBoudingOn() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&propertyName=ADDRESS");
        // print(doc);

        // check it's a feature collection
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        // check the collection has non null bounds
        assertXpathEvaluatesTo(
                "1", "count(//wfs:FeatureCollection/gml:boundedBy/gml:Envelope)", doc);
        // check that each feature has non null bounds
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(
                xpath.getMatchingNodes("//cite:Buildings/gml:boundedBy/gml:Envelope", doc)
                                .getLength()
                        > 0);
    }

    @Test
    public void testFeatureBoudingOff() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(false);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&propertyName=ADDRESS");
        //        print(doc);

        // check it's a feature collection
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        // check the collection does not have bounds
        assertXpathEvaluatesTo("0", "count(//wfs:FeatureCollection/gml:boundedBy)", doc);
    }
}
