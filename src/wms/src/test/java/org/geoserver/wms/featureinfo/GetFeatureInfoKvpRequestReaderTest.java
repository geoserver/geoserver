/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import junit.framework.Test;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.map.GetMapKvpRequestReader;

@SuppressWarnings("unchecked")
public class GetFeatureInfoKvpRequestReaderTest extends KvpRequestReaderTestSupport {
    GetFeatureInfoKvpReader reader;

    Dispatcher dispatcher;

    public static final String STATES_SLD =
            "<StyledLayerDescriptor version=\"1.0.0\">"
                    + "<UserLayer><Name>sf:states</Name><UserStyle><Name>UserSelection</Name>"
                    + "<FeatureTypeStyle><Rule><Filter xmlns:gml=\"http://www.opengis.net/gml\">"
                    + "<PropertyIsEqualTo><PropertyName>STATE_ABBR</PropertyName><Literal>IL</Literal></PropertyIsEqualTo>"
                    + "</Filter><PolygonSymbolizer><Fill><CssParameter name=\"fill\">#FF0000</CssParameter></Fill>"
                    + "</PolygonSymbolizer></Rule><Rule><LineSymbolizer><Stroke/></LineSymbolizer></Rule>"
                    + "</FeatureTypeStyle></UserStyle></UserLayer></StyledLayerDescriptor>";

    /** This is a READ ONLY TEST so we can use one time setup */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureInfoKvpRequestReaderTest());
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

        LayerGroupInfo gi2 = cf.createLayerGroup();
        gi2.setName("testGroup2");
        gi2.getLayers().add(getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart()));
        gi2.getStyles().add(getCatalog().getStyleByName("raster"));
        gi2.getLayers().add(getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart()));
        gi2.getStyles().add(getCatalog().getStyleByName("raster"));
        cb.calculateLayerGroupBounds(gi2);
        getCatalog().add(gi2);
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
        reader = new GetFeatureInfoKvpReader(wms);
    }

    public void testSldDisabled() throws Exception {
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setDynamicStylingDisabled(Boolean.TRUE);
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        reader = new GetFeatureInfoKvpReader(wms);
        GetFeatureInfoRequest request = (GetFeatureInfoRequest) reader.createRequest();
        boolean error = false;
        try {
            request = (GetFeatureInfoRequest) reader.read(request, parseKvp(kvp), kvp);
        } catch (ServiceException e) {
            error = true;
        }
        getGeoServer().remove(info);
        getGeoServer().add(oldInfo);
        assertTrue(error);
    }

    public void testSldBodyDisabled() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("sld_body", STATES_SLD);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setDynamicStylingDisabled(Boolean.TRUE);
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        reader = new GetFeatureInfoKvpReader(wms);
        GetFeatureInfoRequest request = (GetFeatureInfoRequest) reader.createRequest();
        boolean error = false;
        try {
            request = (GetFeatureInfoRequest) reader.read(request, parseKvp(kvp), kvp);
        } catch (ServiceException e) {
            error = true;
        }
        getGeoServer().remove(info);
        getGeoServer().add(oldInfo);
        assertTrue(error);
    }
}
