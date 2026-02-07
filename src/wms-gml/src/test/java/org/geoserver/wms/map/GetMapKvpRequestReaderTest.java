/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geotools.api.filter.Id;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class GetMapKvpRequestReaderTest extends KvpRequestReaderTestSupport {

    GetMapKvpRequestReader reader;

    WMS wms;

    Dispatcher dispatcher;
    public static final String STATES_SLD = "<StyledLayerDescriptor version=\"1.0.0\">"
            + "<UserLayer><Name>sf:states</Name><UserStyle><Name>UserSelection</Name>"
            + "<FeatureTypeStyle><Rule><Filter xmlns:gml=\"http://www.opengis.net/gml\">"
            + "<PropertyIsEqualTo><PropertyName>STATE_ABBR</PropertyName><Literal>IL</Literal></PropertyIsEqualTo>"
            + "</Filter><PolygonSymbolizer><Fill><CssParameter name=\"fill\">#FF0000</CssParameter></Fill>"
            + "</PolygonSymbolizer></Rule><Rule><LineSymbolizer><Stroke/></LineSymbolizer></Rule>"
            + "</FeatureTypeStyle></UserStyle></UserLayer></StyledLayerDescriptor>";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        CatalogFactory cf = getCatalog().getFactory();
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        LayerGroupInfo gi = cf.createLayerGroup();
        gi.setName("testGroup");
        gi.getLayers().add(getCatalog().getLayerByName(BASIC_POLYGONS.getLocalPart()));
        gi.getStyles().add(getCatalog().getStyleByName("polygon"));
        cb.calculateLayerGroupBounds(gi);
        getCatalog().add(gi);

        LayerGroupInfo gi2 = cf.createLayerGroup();
        gi2.setName("testGroup2");
        gi2.getLayers().add(getCatalog().getLayerByName(BASIC_POLYGONS.getLocalPart()));
        gi2.getStyles().add(getCatalog().getStyleByName("raster"));
        gi2.getLayers().add(getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart()));
        gi2.getStyles().add(getCatalog().getStyleByName("raster"));
        cb.calculateLayerGroupBounds(gi2);
        getCatalog().add(gi2);

        LayerGroupInfo gi3 = cf.createLayerGroup();
        gi3.setName("testGroup3");
        gi3.getLayers().add(getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart()));
        gi3.getStyles().add(getCatalog().getStyleByName("raster"));
        gi3.getLayers().add(getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart()));
        gi3.getStyles().add(getCatalog().getStyleByName("raster"));
        cb.calculateLayerGroupBounds(gi3);
        getCatalog().add(gi3);
    }

    @Before
    public void setUpInternal() throws Exception {
        dispatcher = (Dispatcher) applicationContext.getBean("dispatcher");
        GeoServer geoserver = getGeoServer();
        WMSInfo wmsInfo = geoserver.getService(WMSInfo.class);
        WMSInfoImpl impl = (WMSInfoImpl) ModificationProxy.unwrap(wmsInfo);

        impl.setAllowedURLsForAuthForwarding(Collections.singletonList("http://localhost/geoserver/rest/sldurl"));
        wms = new WMS(geoserver);
        reader = new GetMapKvpRequestReader(wms);
    }

    @After
    public void clearEntityResolutionUnrestrictedProperty() {
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
    }

    @Test
    public void testFilter() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("filter", "<Filter><FeatureId id=\"foo\"/></Filter>");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getFilter());
        assertEquals(1, request.getFilter().size());

        Id fid = (Id) request.getFilter().get(0);
        assertEquals(1, fid.getIDs().size());

        assertEquals("foo", fid.getIDs().iterator().next());
    }
}
