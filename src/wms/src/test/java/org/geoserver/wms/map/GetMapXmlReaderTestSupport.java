/* (c) 2013-2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

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
import org.opengis.filter.PropertyIsEqualTo;

public abstract class GetMapXmlReaderTestSupport extends KvpRequestReaderTestSupport {
    AbstractGetMapXmlReader reader;
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

    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        dispatcher = (Dispatcher) applicationContext.getBean("dispatcher");
        WMS wms = new WMS(getGeoServer());
        reader = createReader(wms);
    }

    public void testCreateRequest() throws Exception {
        GetMapRequest request = reader.createRequest();
        assertNotNull(request);
    }

    public void testResolveStylesForLayerGroup(String testFile) throws Exception {
        GetMapRequest request = reader.createRequest();
        BufferedReader input = getResourceInputStream(testFile);

        request = (GetMapRequest) reader.read(request, input, new HashMap());

        String layer = MockData.BASIC_POLYGONS.getLocalPart();
        assertEquals(1, request.getLayers().size());
        assertTrue(request.getLayers().get(0).getName().endsWith(layer));

        assertEquals(1, request.getStyles().size());
        Style expected = getCatalog().getStyleByName("polygon").getStyle();
        Style style = request.getStyles().get(0);
        assertEquals(expected, style);
    }

    public void testLayerFeatureConstraintFilterParsing(String testFile) throws Exception {
        GetMapRequest request = reader.createRequest();
        BufferedReader input = getResourceInputStream(testFile);

        request = (GetMapRequest) reader.read(request, input, new HashMap());

        // Named layer
        String linesLayer = MockData.LINES.getLocalPart();
        assertEquals(1, request.getLayers().size());
        assertTrue(request.getLayers().get(0).getName().endsWith(linesLayer));

        assertEquals(1, request.getFilter().size());
        PropertyIsEqualTo parsed = (PropertyIsEqualTo) request.getFilter().get(0);
        assertEquals("[ NAME = VALUE ]", parsed.toString());
    }

    public void testAllowDynamicStyles(String testFile) throws Exception {
        GetMapRequest request = reader.createRequest();
        BufferedReader input = getResourceInputStream(testFile);

        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setDynamicStylingDisabled(Boolean.TRUE);
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        AbstractGetMapXmlReader reader = createReader(wms);
        boolean error = false;
        try {
            request = (GetMapRequest) reader.read(request, input, new HashMap());
        } catch (ServiceException e) {
            error = true;
        }
        getGeoServer().remove(info);
        getGeoServer().add(oldInfo);
        assertTrue(error);
    }

    protected abstract AbstractGetMapXmlReader createReader(WMS wms);

    private BufferedReader getResourceInputStream(String classRelativePath) throws IOException {
        InputStream resourceStream = getClass().getResource(classRelativePath).openStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(resourceStream));
        return input;
    }
}
