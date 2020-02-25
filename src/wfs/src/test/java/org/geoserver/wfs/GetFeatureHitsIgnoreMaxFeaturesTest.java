/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Test the setting for ignoring max features for hit counts */
public class GetFeatureHitsIgnoreMaxFeaturesTest extends WFSTestSupport {

    /** Check that max features is ignored when the hitsIgnoreMaxFeatures flag is active */
    @Test
    public void testHitsIgnoreMaxFeaturesEnabled() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setMaxFeatures(1);
        wfs.setHitsIgnoreMaxFeatures(true);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.1.0&service=wfs&resultType=hits");
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(
                "15",
                xpath.getMatchingNodes("//wfs:FeatureCollection/@numberOfFeatures", doc)
                        .item(0)
                        .getNodeValue());
    }

    /**
     * Test that doing a GetFeature request for data instead of hits still respects max features
     * with the hitsIgnoreMaxFeatures flag active
     */
    @Test
    public void testGetFeatureRespectsMaxFeatures() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setMaxFeatures(1);
        wfs.setHitsIgnoreMaxFeatures(true);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.1.0&service=wfs");

        // check we get a feature collection
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("cdf:Fifteen");
        // check feature collection has correct count
        assertEquals(1, featureMembers.getLength());
    }

    /** Check that max features is respected when the hitsIgnoreMaxFeatures flag is active */
    @Test
    public void testHitsIgnoreMaxFeaturesDisabled() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setMaxFeatures(1);
        wfs.setHitsIgnoreMaxFeatures(false);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.1.0&service=wfs&resultType=hits");
        // check it's a feature collection
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(
                "1",
                xpath.getMatchingNodes("//wfs:FeatureCollection/@numberOfFeatures", doc)
                        .item(0)
                        .getNodeValue());
    }
}
