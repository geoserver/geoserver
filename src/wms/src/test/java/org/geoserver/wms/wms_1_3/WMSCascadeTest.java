/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSCascadeTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

@RunWith(Parameterized.class)
public class WMSCascadeTest extends WMSCascadeTestSupport {

    private final boolean aphEnabled;

    @Parameters(name = "{index} APH enabled: {0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[] {true}, new Object[] {false});
    }

    public WMSCascadeTest(boolean aphEnabled) {
        this.aphEnabled = aphEnabled;
    }

    @Before
    public void setupAdvancedProjectionHandling() {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, aphEnabled);
        // make sure GetFeatureInfo is not deactivated (this will only update the global service)
        wms.setFeaturesReprojectionDisabled(false);
        gs.save(wms);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // on WMS 1.3 the requested area is enlarged to account for reprojection
        // this is not really needed, it's something we should optimize out.
        // See GEOS-5837 and remove these when it is fixed
        URL pngImage = WMSTestSupport.class.getResource("world.png");
        wms13Client.expectGet(
                new URL(
                        wms13BaseURL
                                + "?service=WMS&version=1.3.0&request=GetMap&layers=world4326"
                                + "&styles&bbox=-110.0,-200.0,110.0,200.0&crs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=190&height=100"),
                new MockHttpResponse(pngImage, "image/png"));
        wms11Client.expectGet(
                new URL(
                        wms11BaseURL
                                + "?service=WMS&version=1.1.1&request=GetMap&layers=world4326"
                                + "&styles&bbox=-200.0,-110.0,200.0,110.0&srs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=190&height=100"),
                new MockHttpResponse(pngImage, "image/png"));

        // setup mocked get feature info (the return features use EPSG:3857)
        URL featureInfo = WMSTestSupport.class.getResource("wms-features.xml");
        wms13Client.expectGet(
                new URL(
                        wms13BaseURL
                                + "?SERVICE=WMS&INFO_FORMAT=application/vnd.ogc.gml&LAYERS=world4326"
                                + "&CRS=EPSG:4326&FEATURE_COUNT=50&FORMAT=image%2Fpng&HEIGHT=101&TRANSPARENT=TRUE&J=-609621&REQUEST=GetFeatureInfo"
                                + "&I=-875268&WIDTH=101&BBOX=-103.829117187,44.3898919295,-103.804563429,44.4069939679&STYLES=&QUERY_LAYERS=world4326&VERSION=1.3.0"),
                new MockHttpResponse(featureInfo, "application/vnd.ogc.gml"));
    }

    @Test
    public void testCascadeGetMapOnto13() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-90,-180,90,180"
                                + "&styles=&layers="
                                + WORLD4326_130
                                + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                                + "&width=180&height=90&crs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }

    @Test
    public void testCascadeGetMapOnto11() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-90,-180,90,180"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                                + "&width=180&height=90&crs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }

    @Test
    public void testCascadeCapabilitiesClientNoGetFeatureInfo() throws Exception {
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.3.0&service=wms");
        print(dom);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("link", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NamespaceContext newNsCtxt = new SimpleNamespaceContext(namespaces);

        xpath.setNamespaceContext(newNsCtxt);

        xpath.evaluate("//wms:Layer[name='" + WORLD4326_110_NFI + "']", dom);
    }

    @Test
    public void testGetFeatureInfoReprojection() throws Exception {
        // do the get feature request using EPSG:4326
        String url =
                "wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true"
                        + "&QUERY_LAYERS="
                        + WORLD4326_130
                        + "&STYLES&LAYERS="
                        + WORLD4326_130
                        + "&INFO_FORMAT=text/xml; subtype=gml/3.1.1"
                        + "&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&WIDTH=101&HEIGHT=101&BBOX=-103.829117187,44.3898919295,-103.804563429,44.4069939679";
        Document result = getAsDOM(url);
        // setup XPATH engine namespaces
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xpath.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        // check the response content, the features should have been reproject from EPSG:3857 to
        // EPSG:4326
        String srs =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("4326"), is(true));
        String rawCoordinates =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/gml:pos/text()",
                        result);
        assertThat(rawCoordinates, notNullValue());
        String[] coordinates = rawCoordinates.split(" ");
        assertThat(coordinates.length, is(2));
        checkNumberSimilar(coordinates[0], 44.39832008, 0.0001);
        checkNumberSimilar(coordinates[1], -103.81711048, 0.0001);
        // deactivate features reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the get feature info request
        result = getAsDOM(url);
        srs =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("3857"), is(true));
        rawCoordinates =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/gml:pos/text()",
                        result);
        assertThat(rawCoordinates, notNullValue());
        coordinates = rawCoordinates.split(" ");
        assertThat(coordinates.length, is(2));
        checkNumberSimilar(coordinates[0], -11556867.874, 0.0001);
        checkNumberSimilar(coordinates[1], 5527291.47718493, 0.0001);
    }
}
