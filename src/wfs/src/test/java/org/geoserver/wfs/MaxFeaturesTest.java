/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class MaxFeaturesTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        // set global max to 5
        GeoServer gs = getGeoServer();

        WFSInfo wfs = getWFS();
        wfs.setMaxFeatures(5);
        gs.save(wfs);
    }

    @Before
    public void resetLocalMaxes() {
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(0);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(0);
        getCatalog().save(info);
    }

    @Test
    public void testGlobalMax() throws Exception {
        // fifteen has 15 elements, but global max is 5
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(5, featureMembers.getLength());
    }

    @Test
    public void testLocalMax() throws Exception {
        // setup different max on local
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(3);
        getCatalog().save(info);

        // fifteen has 15 elements, but global max is 5 and local is 3
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(3, featureMembers.getLength());
    }

    @Test
    public void testLocalMaxBigger() throws Exception {
        // setup different max on local
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(10);
        getCatalog().save(info);

        // fifteen has 15 elements, but global max is 5 and local is 10
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(5, featureMembers.getLength());
    }

    @Test
    public void testCombinedLocalMaxes() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&srsName=EPSG:4326&typename=cdf:Fifteen,cite:BasicPolygons"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(4, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(2, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(2, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }

    @Test
    public void testCombinedLocalMaxesBigger() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(4);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&srsName=EPSG:4326&typename=cdf:Fifteen,cite:BasicPolygons"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(5, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(4, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(1, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }

    @Test
    public void testCombinedLocalMaxesBiggerRequestOverride() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(3);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        getCatalog().save(info);

        info.setMaxFeatures(2);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&srsName=EPSG:4326&typename=cdf:Fifteen,cite:BasicPolygon"
                                + "s&version=1.0.0&service=wfs&maxFeatures=4");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(4, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(3, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(1, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }

    @Test
    public void testMaxFeaturesBreak() throws Exception {
        // See https://osgeo-org.atlassian.net/browse/GEOS-1489
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(3);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen,cite:BasicPolygon"
                                + "s&version=1.0.0&service=wfs&maxFeatures=3");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(3, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(3, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(0, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }
}
