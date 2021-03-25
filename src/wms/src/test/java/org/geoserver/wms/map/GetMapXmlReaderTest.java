/* (c) 2013-2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geotools.styling.Style;
import org.junit.Test;
import org.opengis.filter.PropertyIsEqualTo;

public class GetMapXmlReaderTest extends KvpRequestReaderTestSupport {
    GetMapXmlReader reader;
    Dispatcher dispatcher;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        CatalogFactory cf = getCatalog().getFactory();
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        LayerGroupInfo gi = cf.createLayerGroup();
        gi.setName("testGroup");
        gi.getLayers().add(getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart()));
        gi.getStyles().add(getCatalog().getStyleByName("polygon"));
        cb.calculateLayerGroupBounds(gi);
        getCatalog().add(gi);
    }

    @Override
    protected void oneTimeTearDown() throws Exception {
        super.oneTimeTearDown();
        // reset the legacy flag so that other tests are not getting affected by it
        GeoServerLoader.setLegacy(false);
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        dispatcher = (Dispatcher) applicationContext.getBean("dispatcher");
        WMS wms = new WMS(getGeoServer());
        reader = new GetMapXmlReader(wms);
    }

    @Test
    public void testCreateRequest() throws Exception {
        GetMapRequest request = reader.createRequest();
        assertNotNull(request);
    }

    @Test
    public void testResolveStylesForLayerGroup() throws Exception {
        GetMapRequest request = reader.createRequest();
        try (BufferedReader input =
                getResourceInputStream("WMSPostLayerGroupNonDefaultStyle.xml")) {

            request = (GetMapRequest) reader.read(request, input, new HashMap());

            String layer = MockData.BASIC_POLYGONS.getLocalPart();
            assertEquals(1, request.getLayers().size());
            assertTrue(request.getLayers().get(0).getName().endsWith(layer));

            assertEquals(1, request.getStyles().size());
            Style expected = getCatalog().getStyleByName("polygon").getStyle();
            Style style = request.getStyles().get(0);
            assertEquals(expected, style);
        }
    }

    @Test
    public void testLayerFeatureConstraintFilterParsing() throws Exception {
        GetMapRequest request = reader.createRequest();
        try (BufferedReader input =
                getResourceInputStream("WMSPostLayerFeatureConstraintFilter.xml")) {

            request = (GetMapRequest) reader.read(request, input, new HashMap());

            // Named layer
            String linesLayer = MockData.LINES.getLocalPart();
            assertEquals(1, request.getLayers().size());
            assertTrue(request.getLayers().get(0).getName().endsWith(linesLayer));

            assertEquals(1, request.getFilter().size());
            PropertyIsEqualTo parsed = (PropertyIsEqualTo) request.getFilter().get(0);
            assertEquals("[ NAME = VALUE ]", parsed.toString());
        }
    }

    @Test
    public void testAllowDynamicStyles() throws Exception {
        GetMapRequest request = reader.createRequest();
        try (BufferedReader input =
                getResourceInputStream("WMSPostLayerGroupNonDefaultStyle.xml")) {

            WMS wms = new WMS(getGeoServer());
            WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
            WMSInfo info = new WMSInfoImpl();
            info.setDynamicStylingDisabled(Boolean.TRUE);
            getGeoServer().remove(oldInfo);
            getGeoServer().add(info);
            GetMapXmlReader reader = new GetMapXmlReader(wms);
            boolean error = false;
            try {
                reader.read(request, input, new HashMap());
            } catch (ServiceException e) {
                error = true;
            }
            getGeoServer().remove(info);
            getGeoServer().add(oldInfo);
            assertTrue(error);
        }
    }

    @SuppressWarnings("PMD.CloseResource") // wrapped and returned
    private BufferedReader getResourceInputStream(String classRelativePath) throws IOException {
        InputStream resourceStream = getClass().getResource(classRelativePath).openStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(resourceStream));
        return input;
    }
}
