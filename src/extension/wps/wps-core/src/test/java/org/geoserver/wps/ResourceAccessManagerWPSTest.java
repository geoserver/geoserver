/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;

public class ResourceAccessManagerWPSTest extends WPSTestSupport {

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wps/ResourceAccessManagerContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings =
                catalog.getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));

        // limits make the layer be visible when logged in as the cite user, but not when
        // running as the anonymous one (the TestResourceAccessManager does not allow
        // to run tests against un-recognized users)
        tam.putLimits(
                "cite",
                buildings,
                new VectorAccessLimits(CatalogMode.HIDE, null, Filter.INCLUDE, null, null));
        tam.putLimits(
                "anonymous",
                buildings,
                new VectorAccessLimits(CatalogMode.HIDE, null, Filter.EXCLUDE, null, null));
    }

    @Test
    public void testDenyAccess() throws Exception {
        Document dom = runBuildingsRequest();
        // print(dom);

        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("0", xp.evaluate("count(//wps:ProcessSucceded)", dom));
    }

    @Test
    public void testAllowAccess() throws Exception {
        setRequestAuth("cite", "cite");
        Document dom = runBuildingsRequest();
        // print(dom);

        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
        String[] lc =
                xp.evaluate(
                                "//wps:Output[ows:Identifier = 'bounds']/wps:Data/wps:BoundingBoxData/ows:LowerCorner",
                                dom)
                        .split("\\s+");
        assertEquals(8.0E-4, Double.parseDouble(lc[0]), 0d);
        assertEquals(5.0E-4, Double.parseDouble(lc[1]), 0d);
        String[] uc =
                xp.evaluate(
                                "//wps:Output[ows:Identifier = 'bounds']/wps:Data/wps:BoundingBoxData/ows:UpperCorner",
                                dom)
                        .split("\\s+");
        assertEquals(0.0024, Double.parseDouble(uc[0]), 0d);
        assertEquals(0.001, Double.parseDouble(uc[1]), 0d);
    }

    private Document runBuildingsRequest() throws Exception {
        // @formatter:off
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getLayerId(MockData.BUILDINGS)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:Output>\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:Output>\n"
                        + " </wps:ResponseForm>\n"
                        + "</wps:Execute>";
        // @formatter:on

        Document dom = postAsDOM("wps", xml);
        return dom;
    }
}
