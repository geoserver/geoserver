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

import junit.framework.Test;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geotools.styling.Style;

public class GetMapXmlReaderTest extends KvpRequestReaderTestSupport {
    GetMapXmlReader reader;
    Dispatcher dispatcher;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetMapXmlReaderTest());
    }

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
        reader = new GetMapXmlReader(wms);
    }
    
    public void testCreateRequest() throws Exception {
        GetMapRequest request = (GetMapRequest) reader.createRequest();
        assertNotNull(request);
    }
    
    public void testResolveStylesForLayerGroup() throws Exception {
        GetMapRequest request = (GetMapRequest) reader.createRequest();
        BufferedReader input = getResourceInputStream("WMSPostLayerGroupNonDefaultStyle.xml");

        request = (GetMapRequest) reader.read(request, input, new HashMap());
        
        String layer = MockData.BASIC_POLYGONS.getLocalPart();
        assertEquals(1, request.getLayers().size());
        assertTrue(request.getLayers().get(0).getName().endsWith(layer));

        assertEquals(1, request.getStyles().size());
        Style expected = getCatalog().getStyleByName("polygon").getStyle();
        Style style = request.getStyles().get(0);
        assertEquals(expected, style);
    }

    private BufferedReader getResourceInputStream(String classRelativePath) throws IOException {
        InputStream resourceStream = getClass().getResource(classRelativePath).openStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(resourceStream));
        return input;
    }

}