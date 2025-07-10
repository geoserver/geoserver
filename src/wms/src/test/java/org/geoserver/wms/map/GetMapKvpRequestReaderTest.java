/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.kvp.URIKvpParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geoserver.wms.CacheConfiguration;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.kvp.PaletteManager;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.style.Style;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class GetMapKvpRequestReaderTest extends KvpRequestReaderTestSupport {

    private static final Logger LOG = Logging.getLogger(GetMapKvpRequestReaderTest.class);

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
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

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
        GeoServer geoserver = getGeoServer();
        WMSInfo wmsInfo = geoserver.getService(WMSInfo.class);
        WMSInfoImpl impl = (WMSInfoImpl) ModificationProxy.unwrap(wmsInfo);

        impl.setAllowedURLsForAuthForwarding(Collections.singletonList("http://localhost/geoserver/rest/sldurl"));
        wms = new WMS(geoserver);
        reader = new GetMapKvpRequestReader(wms);
    }

    @Test
    public void testSldEntityResolver() throws Exception {
        WMS wms = new WMS(getGeoServer());
        GeoServerInfo geoserverInfo = wms.getGeoServer().getGlobal();
        try {
            // enable entities in external SLD files
            geoserverInfo.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(geoserverInfo);

            // test setting has been saved
            assertNotNull(wms.getGeoServer().getGlobal().isXmlExternalEntitiesEnabled());
            assertTrue(wms.getGeoServer().getGlobal().isXmlExternalEntitiesEnabled());

            // test no custom entity resolver will be used
            GetMapKvpRequestReader reader = new GetMapKvpRequestReader(wms);
            assertNull(reader.getEntityResolverProvider().getEntityResolver());

            // disable entities
            geoserverInfo.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(geoserverInfo);

            // since XML entities are disabled for external SLD files
            // I need an entity resolver which enforce this
            reader = new GetMapKvpRequestReader(wms);
            assertNotNull(reader.getEntityResolverProvider().getEntityResolver());

            // try default value: entities should be disabled
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);

            reader = new GetMapKvpRequestReader(wms);
            assertNotNull(reader.getEntityResolverProvider().getEntityResolver());
        } finally {
            // reset to default
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);
        }
    }

    @Test
    public void testCreateRequest() throws Exception {
        GetMapRequest request = reader.createRequest();
        assertNotNull(request);
    }

    @Test
    public void testReadMandatory() throws Exception {
        HashMap raw = new HashMap<>();
        raw.put("layers", getLayerId(BASIC_POLYGONS));
        raw.put("styles", BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        String layer = BASIC_POLYGONS.getLocalPart();
        assertEquals(1, request.getLayers().size());
        assertTrue(request.getLayers().get(0).getName().endsWith(layer));

        assertEquals(1, request.getStyles().size());
        Style expected = getCatalog().getStyleByName(layer).getStyle();
        Style style = request.getStyles().get(0);
        assertEquals(expected, style);

        assertEquals("image/jpeg", request.getFormat());
        assertEquals(600, request.getHeight());
        assertEquals(800, request.getWidth());

        assertNotNull(request.getBbox());
        assertEquals(-10d, request.getBbox().getMinX(), 0);
        assertEquals(-10d, request.getBbox().getMinY(), 0);
        assertEquals(10d, request.getBbox().getMaxX(), 0);
        assertEquals(10d, request.getBbox().getMaxY(), 0);

        assertEquals("epsg:3003", request.getSRS());
    }

    @Test
    public void testReadOptional() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("bgcolor", "000000");
        kvp.put("transparent", "true");
        kvp.put("tiled", "true");
        kvp.put("tilesorigin", "1.2,3.4");
        kvp.put("buffer", "1");
        kvp.put("palette", "SAFE");
        kvp.put("time", "2006-02-27T22:08:12Z");
        kvp.put("elevation", "4");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertEquals(Color.BLACK, request.getBgColor());
        assertTrue(request.isTransparent());
        assertTrue(request.isTiled());

        assertEquals(new Point2D.Double(1.2, 3.4), request.getTilesOrigin());
        assertEquals(1, request.getBuffer());

        assertEquals(PaletteManager.safePalette, request.getPalette());
        assertEquals(Arrays.asList(4.0), request.getElevation());

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2006, 1, 27, 22, 8, 12);
        List<Object> times = request.getTime();
        assertEquals(1, request.getTime().size());
        assertEquals(cal.getTime(), ((DateRange) times.get(0)).getMinValue());
    }

    @Test
    public void testDefaultStyle() throws Exception {
        HashMap raw = new HashMap<>();
        raw.put(
                "layers",
                BASIC_POLYGONS.getPrefix()
                        + ":"
                        + BASIC_POLYGONS.getLocalPart()
                        + ","
                        + MockData.BUILDINGS.getPrefix()
                        + ":"
                        + MockData.BUILDINGS.getLocalPart());
        raw.put("styles", ",");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
        assertEquals(2, request.getStyles().size());
        LayerInfo basicPolygons = getCatalog().getLayerByName(BASIC_POLYGONS.getLocalPart());
        LayerInfo buildings = getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart());
        assertEquals(
                basicPolygons.getDefaultStyle().getStyle(), request.getStyles().get(0));
        assertEquals(buildings.getDefaultStyle().getStyle(), request.getStyles().get(1));
    }

    @Test
    public void testInterpolations() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("interpolations", "bicubic");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(1, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertTrue(request.getInterpolations().get(0) instanceof InterpolationBicubic);

        kvp.put(
                "layers",
                getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS));
        kvp.put("interpolations", "bicubic,,bilinear");
        request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(3, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertNull(request.getInterpolations().get(1));
        assertNotNull(request.getInterpolations().get(2));
        assertTrue(request.getInterpolations().get(2) instanceof InterpolationBilinear);
    }

    @Test
    public void testFiltersForLayerGroups() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("layers", "testGroup3");
        kvp.put("cql_filter", "ADDRESS='123 Main Street'");
        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
        assertNotNull(request.getFilter());
        assertEquals(2, request.getFilter().size());
        assertNotNull(request.getCQLFilter());
        assertEquals(2, request.getCQLFilter().size());
    }

    @Test
    public void testInterpolationsForLayerGroups() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("layers", "testGroup2");
        kvp.put("interpolations", "bicubic");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(2, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertTrue(request.getInterpolations().get(0) instanceof InterpolationBicubic);

        assertNotNull(request.getInterpolations().get(1));
        assertTrue(request.getInterpolations().get(1) instanceof InterpolationBicubic);

        kvp.put("layers", "testGroup2,testGroup," + getLayerId(BASIC_POLYGONS));
        kvp.put("interpolations", "bicubic,bilinear,nearest neighbor");

        request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(4, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertTrue(request.getInterpolations().get(0) instanceof InterpolationBicubic);
        assertNotNull(request.getInterpolations().get(1));
        assertTrue(request.getInterpolations().get(1) instanceof InterpolationBicubic);
        assertNotNull(request.getInterpolations().get(2));
        assertTrue(request.getInterpolations().get(2) instanceof InterpolationBilinear);
        assertNotNull(request.getInterpolations().get(3));
        assertTrue(request.getInterpolations().get(3) instanceof InterpolationNearest);

        kvp.put("layers", "testGroup2,testGroup," + getLayerId(BASIC_POLYGONS));
        kvp.put("interpolations", ",bilinear");

        request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(4, request.getInterpolations().size());
        assertNull(request.getInterpolations().get(0));
        assertNull(request.getInterpolations().get(1));
        assertNotNull(request.getInterpolations().get(2));
        assertTrue(request.getInterpolations().get(2) instanceof InterpolationBilinear);
        assertNull(request.getInterpolations().get(3));
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

    @Test
    public void testCQLFilter() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("cql_filter", "foo = bar");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getCQLFilter());
        assertEquals(1, request.getCQLFilter().size());

        assertThat(request.getCQLFilter().get(0), CoreMatchers.instanceOf(PropertyIsEqualTo.class));
    }

    @Test
    public void testFeatureId() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("featureid", "foo");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getFeatureId());
        assertEquals(1, request.getFeatureId().size());

        // TODO: request.getFeatureId() returns List<FeatureId> but it's not being parsed and contains Strings instead
        List<?> featureId = request.getFeatureId();
        assertEquals("foo", featureId.get(0));
    }

    @Test
    public void testSortBy() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("sortBy", "FID D");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertEquals(1, request.getSortBy().size());

        List<SortBy> sort = request.getSortBy().get(0);
        assertEquals(1, sort.size());
        assertSortBy(sort.get(0), "FID", SortOrder.DESCENDING);
    }

    @Test
    public void testSortByGroup() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("layers", "testGroup2");
        kvp.put("sortBy", "FID D");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertEquals(2, request.getSortBy().size());

        List<SortBy> sort1 = request.getSortBy().get(0);
        assertEquals(1, sort1.size());
        assertSortBy(sort1.get(0), "FID", SortOrder.DESCENDING);

        List<SortBy> sort2 = request.getSortBy().get(0);
        assertEquals(1, sort2.size());
        assertSortBy(sort2.get(0), "FID", SortOrder.DESCENDING);
    }

    @Test
    public void testSortByLessThanRequired() throws Exception {
        Map<String, Object> kvp = new HashMap<>();
        kvp.put("layers", getLayerId(BASIC_POLYGONS) + "," + getLayerId(MockData.LAKES));
        kvp.put("sortBy", "FID D");

        GetMapRequest request = reader.createRequest();
        try {
            reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("sortBy"));
        }
    }

    private void assertSortBy(SortBy sort, String propertyName, SortOrder direction) {
        assertEquals(propertyName, sort.getPropertyName().getPropertyName());
        assertEquals(direction, sort.getSortOrder());
    }

    @Test
    public void testSldNoDefault() throws Exception {
        // no style name, no default, we should fall back on the server default
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        // the kvp should be already in decoded form
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));

        GetMapRequest request = reader.createRequest();
        reader.setLaxStyleMatchAllowed(false);
        request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getSld());
        assertEquals(URIKvpParser.uriEncode(decoded), request.getSld().toURL().toExternalForm());
        final Style style = request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("BasicPolygons", style.getName());
    }

    @Test
    public void testSldDefault() throws Exception {
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URIKvpParser.uriEncode(decoded), request.getSld().toURL().toExternalForm());
        final Style style = request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
    }

    @Test
    public void testSldCache() throws Exception {
        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setCacheConfiguration(new CacheConfiguration(true));
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        URL sld = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        StringBuilder builder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sld.openStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) builder.append(inputLine);
        }

        MockHttpClientConnectionManager manager = new MockHttpClientConnectionManager(builder.toString(), true);
        reader = new GetMapKvpRequestReader(wms, manager);
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap<>();
        String url = "http://cached_sld";
        kvp.put("sld", url);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(1, manager.getConnections());

        request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        // no connection is done, the result is taken from cache
        assertEquals(1, manager.getConnections());
    }

    @Test
    public void testSldCacheNotEnabled() throws Exception {
        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setCacheConfiguration(new CacheConfiguration(true));
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        URL sld = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        StringBuilder builder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sld.openStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) builder.append(inputLine);
        }

        MockHttpClientConnectionManager manager = new MockHttpClientConnectionManager(builder.toString(), false);
        reader = new GetMapKvpRequestReader(wms, manager);
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap<>();
        String url = "http://cached_sld";
        kvp.put("sld", url);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(1, manager.getConnections());

        request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        // new connection is done, the result is NOT taken from cache
        assertEquals(2, manager.getConnections());
    }

    @Test
    public void testSldDisabled() throws Exception {
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));

        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setDynamicStylingDisabled(Boolean.TRUE);
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        reader = new GetMapKvpRequestReader(wms);
        GetMapRequest request = reader.createRequest();
        boolean error = false;
        try {
            request = reader.read(request, parseKvp(kvp), kvp);
        } catch (ServiceException e) {
            error = true;
        }
        getGeoServer().remove(info);
        getGeoServer().add(oldInfo);
        assertTrue(error);
    }

    @Test
    public void testSldBodyDisabled() throws Exception {
        HashMap kvp = new HashMap<>();
        kvp.put("sld_body", STATES_SLD);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));

        WMS wms = new WMS(getGeoServer());
        WMSInfo oldInfo = wms.getGeoServer().getService(WMSInfo.class);
        WMSInfo info = new WMSInfoImpl();
        info.setDynamicStylingDisabled(Boolean.TRUE);
        getGeoServer().remove(oldInfo);
        getGeoServer().add(info);
        reader = new GetMapKvpRequestReader(wms);
        GetMapRequest request = reader.createRequest();
        boolean error = false;
        try {
            request = reader.read(request, parseKvp(kvp), kvp);
        } catch (ServiceException e) {
            error = true;
        }
        getGeoServer().remove(info);
        getGeoServer().add(oldInfo);
        assertTrue(error);
    }

    @Test
    public void testSldNamed() throws Exception {
        // style name matching one in the sld
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "TheLibraryModeStyle");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URIKvpParser.uriEncode(decoded), request.getSld().toURL().toExternalForm());
        final Style style = request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
    }

    @Test
    public void testSldFailLookup() throws Exception {
        // nothing matches the required style name
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            // System.out.println(e);
        }
    }

    @Test
    public void testSldConnectionFailure() throws Exception {
        // Connection for specified external SLD fails while retrieving SLD
        HashMap kvp = new HashMap<>();

        URL url = new URL("http://hostthatdoesnotexist/");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertNull("Exception should not reveal its cause", e.getCause());
        }
    }

    @Test
    public void testSldRemoteHttp() throws Exception {
        final String fakeBasicAuth = "FakeBasicAuth";
        final String authHeader = "Authorization";
        BasicHeader expectedBasicHeader = new BasicHeader(authHeader, fakeBasicAuth);

        GetMapRequest request = reader.createRequest();
        GetMapRequest spyRequest = spy(request);
        when(spyRequest.getHttpRequestHeader(authHeader)).thenReturn(fakeBasicAuth);
        Header[] headers = new Header[1];

        // Using an allowed URL
        URL url = new URL("http://localhost/geoserver/rest/sldurl/sample.sld");
        requestWithHeaders(spyRequest, url, headers, authHeader);
        assertSameHeader(headers, expectedBasicHeader);

        // Using a not allowed URL
        url = new URL("http://not-allowed-endpoint/sample.sld");
        requestWithHeaders(spyRequest, url, headers, authHeader);
        // No header will be forwarded when not allowed
        assertSameHeader(headers, null);
    }

    private void requestWithHeaders(GetMapRequest spyRequest, URL url, Header[] headers, String authHeader)
            throws Exception {
        String urlDecoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        HashMap kvp = new HashMap<>();
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("sld", urlDecoded);

        try (InputStream sld = GetMapKvpRequestReader.class.getResourceAsStream("BasicPolygonsLibraryNoDefault.sld")) {

            GetMapKvpRequestReader reqReader = new GetMapKvpRequestReader(wms, null) {

                @Override
                protected CloseableHttpResponse executeRequest(HttpCacheContext cacheContext, HttpGet httpget)
                        throws IOException {
                    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
                    HttpEntity entity = mock(HttpEntity.class);

                    when(response.getEntity()).thenReturn(entity);
                    when(entity.getContent()).thenReturn(sld);

                    Header[] httpHeaders = httpget.containsHeader(authHeader) ? httpget.getHeaders(authHeader) : null;
                    headers[0] = httpHeaders != null ? httpHeaders[0] : null;
                    return response;
                }
            };
            reqReader.read(spyRequest, parseKvp(kvp), caseInsensitiveKvp(kvp));
        }
    }

    private void assertSameHeader(Header[] headers, BasicHeader expectedBasicHeader) {
        if (expectedBasicHeader == null) {
            assertNull(headers[0]);
        } else {
            assertEquals(expectedBasicHeader.toString(), headers[0].toString());
        }
    }

    @Test
    public void testSldNotExist() throws Exception {
        // Specified external SLD does not exist
        HashMap kvp = new HashMap<>();

        URL url = new URL(GetMapKvpRequestReaderTest.class.getResource(""), "does-not-exist");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertNull("Exception should not reveal its cause", e.getCause());
        }
    }

    @Test
    public void testSldNotXML() throws Exception {
        // Specified external SLD is not XML
        HashMap kvp = new HashMap<>();

        URL url = GetMapKvpRequestReaderTest.class.getResource("paletted.tif");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertNull("Exception should not reveal its cause", e.getCause());
        }
    }

    @Test
    public void testSldNotSld() throws Exception {
        // Specified external SLD is XML that is not SLD
        HashMap kvp = new HashMap<>();

        URL url = GetMapKvpRequestReaderTest.class.getResource("WMSPostLayerGroupNonDefaultStyle.xml");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertNull("Exception should not reveal its cause", e.getCause());
        }
    }

    @Test
    public void testSldFeatureTypeConstraints() throws Exception {
        // no styles, no layer, the full definition is in the sld
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsFeatureTypeConstaint.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URIKvpParser.uriEncode(decoded), request.getSld().toURL().toExternalForm());
        // check the style
        final Style style = request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
        // check the layer
        assertEquals(1, request.getLayers().size());
        MapLayerInfo layer = request.getLayers().get(0);
        assertEquals(getLayerId(BASIC_POLYGONS), layer.getName());
        // check the filter imposed in the feature type constraint
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        assertEquals(1, layer.getLayerFeatureConstraints().length);
        assertEquals(
                ff.equals(ff.property("ID"), ff.literal("xyz")), layer.getLayerFeatureConstraints()[0].getFilter());
    }

    @Test
    public void testSldLibraryFeatureTypeConstraints() throws Exception {
        // no styles, no layer, the full definition is in the sld
        HashMap kvp = new HashMap<>();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsFeatureTypeConstaint.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put("layers", getLayerId(BASIC_POLYGONS));
        kvp.put("styles", "TheLibraryModeStyle");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URIKvpParser.uriEncode(decoded), request.getSld().toURL().toExternalForm());
        // check the style
        final Style style = request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
        // check the layer
        assertEquals(1, request.getLayers().size());
        MapLayerInfo layer = request.getLayers().get(0);
        assertEquals(getLayerId(BASIC_POLYGONS), layer.getName());
        // check the filter imposed in the feature type constraint
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        assertEquals(1, layer.getLayerFeatureConstraints().length);
        assertEquals(
                ff.equals(ff.property("ID"), ff.literal("xyz")), layer.getLayerFeatureConstraints()[0].getFilter());
    }

    /** One of the cite tests ensures that WMTVER is recognized as VERSION and the server does not complain */
    @Test
    public void testWmtVer() throws Exception {
        dispatcher.setCiteCompliant(true);
        String request =
                "wms?SERVICE=WMS&&WiDtH=200&FoRmAt=image/png&LaYeRs=cite:Lakes&StYlEs=&BbOx=0,-0.0020,0.0040,0&ReQuEsT=GetMap&HeIgHt=100&SrS=EPSG:4326&WmTvEr=1.1.1";
        assertEquals("image/png", getAsServletResponse(request).getContentType());
    }

    @Test
    public void testRemoteWFS() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        HashMap raw = new HashMap<>();
        raw.put("layers", "topp:states");
        raw.put("styles", BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", RemoteOWSTestSupport.WFS_SERVER_URL);

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        assertEquals("WFS", request.getRemoteOwsType()); // TODO: handle case?
        assertEquals(new URL(RemoteOWSTestSupport.WFS_SERVER_URL), request.getRemoteOwsURL());
        assertEquals(1, request.getLayers().size());
        assertEquals(
                PublishedType.REMOTE.getCode().intValue(),
                request.getLayers().get(0).getType());
        assertEquals("topp:states", request.getLayers().get(0).getName());
    }

    @Test
    public void testRemoteWFSNoStyle() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        HashMap raw = new HashMap<>();
        raw.put("layers", "topp:states");
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", RemoteOWSTestSupport.WFS_SERVER_URL);

        GetMapRequest request = reader.createRequest();
        try {
            request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            fail("This should have thrown an exception because of the missing style");
        } catch (ServiceException e) {
            assertEquals("NoDefaultStyle", e.getCode());
        }
    }

    @Test
    public void testRemoteWFSInvalidURL() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        HashMap raw = new HashMap<>();
        raw.put("layers", "topp:states");
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", "http://phantom.openplans.org:8080/crapserver/wfs?");

        GetMapRequest request = reader.createRequest();
        try {
            request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            fail("This should have thrown an exception because of the non existent layer");
        } catch (ServiceException e) {
            assertEquals("RemoteOWSFailure", e.getCode());
        }
    }

    @Test
    public void testGroupInSLD() throws Exception {
        // see GEOS-1818
        final HashMap kvp = new HashMap<>();
        kvp.put("srs", "epsg:4326");
        kvp.put("bbox", "124.38035938267053,-58.45445933799711,169.29632161732948,-24.767487662002893");
        kvp.put("width", "640");
        kvp.put("height", "480");
        kvp.put("format", "image/png");
        final URL url = GetMapKvpRequestReader.class.getResource("BaseMapGroup.sld");
        // URLDecoder.decode fixes GEOS-3709
        kvp.put("sld", URLDecoder.decode(url.toString(), "UTF-8"));
        kvp.put("version", "1.1.1");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertEquals(1, request.getLayers().size());
        assertEquals(1, request.getStyles().size());
        assertEquals(getLayerId(BASIC_POLYGONS), request.getLayers().get(0).getName());
        Style expectedStyle = getCatalog().getStyleByName("polygon").getStyle();
        assertEquals(expectedStyle, request.getStyles().get(0));
    }

    @Test
    public void testViewParams() throws Exception {
        HashMap raw = new HashMap<>();
        raw.put("layers", getLayerId(BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(1, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
    }

    @Test
    public void testMultipleViewParams() throws Exception {
        HashMap raw = new HashMap<>();
        raw.put("layers", getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD,where:WHERE PERSONS > 10;str:FOO");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(2, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
        viewParams = viewParamsList.get(1);
        assertEquals("WHERE PERSONS > 10", viewParams.get("where"));
        assertEquals("FOO", viewParams.get("str"));
    }

    /** Base request without layers */
    private HashMap setupBaseViewParamsRequest() {
        HashMap raw = new HashMap<>();
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        return raw;
    }

    @Test
    public void testFanOutViewParams() throws Exception {
        HashMap raw = setupBaseViewParamsRequest();
        raw.put("layers", getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(2, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
        viewParams = viewParamsList.get(1);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
    }

    @Test
    public void testFanOutViewParamsGroupFirst() throws Exception {
        HashMap raw = setupBaseViewParamsRequest();
        raw.put("layers", "testGroup2," + getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS));
        raw.put("viewParams", "test:123,where:WHERE PERSONS > 1000000,str:ABCD");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(4, viewParamsList.size());
        assertEquals(Map.of("test", "123"), viewParamsList.get(0));
        assertEquals(Map.of("test", "123"), viewParamsList.get(1));
        assertEquals(Map.of("where", "WHERE PERSONS > 1000000"), viewParamsList.get(2));
        assertEquals(Map.of("str", "ABCD"), viewParamsList.get(3));
    }

    @Test
    public void testFanOutViewParamsGroupMid() throws Exception {
        HashMap raw = setupBaseViewParamsRequest();
        raw.put("layers", getLayerId(BASIC_POLYGONS) + ",testGroup2," + getLayerId(BASIC_POLYGONS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000,test:123,str:ABCD");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(4, viewParamsList.size());
        assertEquals(Map.of("where", "WHERE PERSONS > 1000000"), viewParamsList.get(0));
        assertEquals(Map.of("test", "123"), viewParamsList.get(1));
        assertEquals(Map.of("test", "123"), viewParamsList.get(2));
        assertEquals(Map.of("str", "ABCD"), viewParamsList.get(3));
    }

    @Test
    public void testFanOutViewParamsGroupLast() throws Exception {
        HashMap raw = setupBaseViewParamsRequest();
        raw.put("layers", getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS) + ",testGroup2");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000,str:ABCD,test:123");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(4, viewParamsList.size());
        assertEquals(Map.of("where", "WHERE PERSONS > 1000000"), viewParamsList.get(0));
        assertEquals(Map.of("str", "ABCD"), viewParamsList.get(1));
        assertEquals(Map.of("test", "123"), viewParamsList.get(2));
        assertEquals(Map.of("test", "123"), viewParamsList.get(3));
    }

    @Test
    public void testMissingLayersAndStylesParametersWithSld() throws Exception {
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");

        // Fix [GEOS-9646]: INSPIRE validation get errors of GetMapRequest parameters.
        HashMap raw = new HashMap<>();
        raw.put("sld", decoded);
        raw.put("format", "image/jpeg");
        raw.put("crs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("transparent", "true");
        raw.put("request", "GetMap");
        raw.put("version", "1.3.0");

        GeoServer geoServer = getGeoServer();
        WMSInfo service = geoServer.getService(WMSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);

        try {
            GetMapRequest request = reader.createRequest();
            reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
        } finally {
            service.setCiteCompliant(false);
            geoServer.save(service);
        }
    }

    @Test
    public void testMissingCrsParameterInGetMapRequest11() throws Exception {
        // Fix [GEOS-9646]: INSPIRE validation get errors of GetMapRequest parameters.
        HashMap raw = new HashMap<>();
        raw.put("layers", getLayerId(BASIC_POLYGONS));
        raw.put("styles", BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("transparent", "true");
        raw.put("request", "GetMap");
        raw.put("version", "1.1.0");

        GeoServer geoServer = getGeoServer();
        WMSInfo service = geoServer.getService(WMSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);

        try {
            GetMapRequest request = reader.createRequest();
            reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
        } finally {
            service.setCiteCompliant(false);
            geoServer.save(service);
        }
    }

    private void validateMissingParameterInGetMapRequest13(String paramToRemove) throws Exception {
        // Fix [GEOS-9646]: INSPIRE validation get errors of GetMapRequest parameters.
        HashMap raw = new HashMap<>();
        raw.put("layers", getLayerId(BASIC_POLYGONS));
        raw.put("styles", BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/jpeg");
        raw.put("crs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("transparent", "true");
        raw.put("request", "GetMap");
        raw.put("version", "1.3.0");
        raw.remove(paramToRemove);

        GeoServer geoServer = getGeoServer();
        WMSInfo service = geoServer.getService(WMSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);

        try {
            GetMapRequest request = reader.createRequest();
            reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            throw new Exception("Shouldn't get here");
        } catch (Exception e) {
            assertThat(e.getMessage(), not(containsString("Shouldn't get here")));
        }
        service.setCiteCompliant(false);
        geoServer.save(service);
    }

    @Test
    public void testMissingStylesParameterInGetMapRequest13() throws Exception {
        validateMissingParameterInGetMapRequest13("styles");
    }

    @Test
    public void testMissingCrsParameterInGetMapRequest13() throws Exception {
        validateMissingParameterInGetMapRequest13("crs");
    }

    @Test
    public void testTransparencyValueInInspireGetMapRequest() throws Exception {
        // Fix [GEOS-9646]: INSPIRE validation get errors of GetMapRequest parameters.
        HashMap raw = new HashMap<>();
        raw.put("layers", getLayerId(BASIC_POLYGONS));
        raw.put("styles", BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/jpeg");
        raw.put("crs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("transparent", "ZZZZZZ");
        raw.put("request", "GetMap");

        GeoServer geoServer = getGeoServer();
        WMSInfo service = geoServer.getService(WMSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);

        try {
            GetMapRequest request = reader.createRequest();
            reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            throw new Exception("Shouldn't get here");
        } catch (Exception e) {
            assertThat(e.getMessage(), not(containsString("Shouldn't get here")));
        }
        service.setCiteCompliant(false);
        geoServer.save(service);
    }

    /** Tests the timeout parameter and the max execution time. */
    @Test
    public void testSldTooLongLookup() throws Exception {
        HttpServer server = createServer();
        GeoServer geoServer = this.getGeoServer();
        WMSInfo wmsInfo = geoServer.getService(WMSInfo.class);
        wmsInfo.setRemoteStyleMaxRequestTime(1000);
        geoServer.save(wmsInfo);
        try {
            WMS wms = new WMS(getGeoServer());
            reader = new GetMapKvpRequestReader(wms);
            server.start();
            int port = server.getAddress().getPort();

            // nothing matches the required style name
            HashMap kvp = new HashMap<>();
            URL url = new URL("http://localhost:" + port + "/sld/style.sld");
            kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
            kvp.put("layers", getLayerId(BASIC_POLYGONS));
            kvp.put("styles", "ThisStyleDoesNotExists");

            GetMapRequest request = reader.createRequest();
            Instant startInstant = Instant.now();
            try {
                reader.setLaxStyleMatchAllowed(false);
                request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
                fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
            } catch (ServiceException e) {
                LOG.log(Level.INFO, e.getMessage(), e);
            }
            long millis = Instant.now().toEpochMilli() - startInstant.toEpochMilli();
            assertTrue("Max timeout should be 2 seconds", millis < 2000);
        } finally {
            server.stop(0);
            wmsInfo = geoServer.getService(WMSInfo.class);
            wmsInfo.setRemoteStyleMaxRequestTime(60000);
            geoServer.save(wmsInfo);
        }
    }

    /** Tests the timeout parameter. */
    @Test
    public void testSldTimeoutLookup() throws Exception {
        HttpServer server = createServer();
        GeoServer geoServer = this.getGeoServer();
        WMSInfo wmsInfo = geoServer.getService(WMSInfo.class);
        wmsInfo.setRemoteStyleTimeout(1000);
        geoServer.save(wmsInfo);
        try {
            WMS wms = new WMS(getGeoServer());
            reader = new GetMapKvpRequestReader(wms);
            server.start();
            int port = server.getAddress().getPort();

            // nothing matches the required style name
            HashMap kvp = new HashMap<>();
            URL url = new URL("http://localhost:" + port + "/sld/style.sld");
            kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
            kvp.put("layers", getLayerId(BASIC_POLYGONS));
            kvp.put("styles", "ThisStyleDoesNotExists");

            GetMapRequest request = reader.createRequest();
            Instant startInstant = Instant.now();
            try {
                reader.setLaxStyleMatchAllowed(false);
                request = reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
                fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
            } catch (ServiceException e) {
                LOG.log(Level.INFO, e.getMessage(), e);
            }
            long millis = Instant.now().toEpochMilli() - startInstant.toEpochMilli();
            assertTrue("Max timeout should be 2 seconds", millis < 2000);
        } finally {
            server.stop(0);
            wmsInfo = geoServer.getService(WMSInfo.class);
            wmsInfo.setRemoteStyleTimeout(30000);
            geoServer.save(wmsInfo);
        }
    }

    @Test
    public void testXMLMultipleViewParams() throws Exception {
        try {
            Request owsRequest = new Request();
            owsRequest.setRawKvp(new HashMap<>());
            owsRequest.getRawKvp().put("viewParamsFormat", "XML");
            Dispatcher.REQUEST.set(owsRequest);
            HashMap raw = new HashMap<>();
            raw.put("layers", getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS));
            raw.put("styles", "");
            raw.put("format", "image/jpeg");
            raw.put("srs", "epsg:3003");
            raw.put("bbox", "-10,-10,10,10");
            raw.put("height", "600");
            raw.put("width", "800");
            raw.put("request", "GetMap");
            raw.put("service", "wms");
            raw.put(
                    "viewParams",
                    "<VP><PS><P n=\"where\">WHERE PERSONS &gt; 1000000</P><P n=\"str\">ABCD</P></PS>"
                            + "<PS><P n=\"where\">WHERE PERSONS &gt; 10</P><P n=\"str\">FOO</P></PS></VP>");

            GetMapRequest request = reader.createRequest();
            request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

            List<Map<String, String>> viewParamsList = request.getViewParams();
            assertEquals(2, viewParamsList.size());
            Map viewParams = viewParamsList.get(0);
            assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
            assertEquals("ABCD", viewParams.get("str"));
            viewParams = viewParamsList.get(1);
            assertEquals("WHERE PERSONS > 10", viewParams.get("where"));
            assertEquals("FOO", viewParams.get("str"));
        } finally {
            Dispatcher.REQUEST.set(null);
        }
    }

    @Test
    public void testXMLMultipleViewParamsServiceException() throws Exception {
        ServiceException serviceException = null;
        try {
            Request owsRequest = new Request();
            owsRequest.setRawKvp(new HashMap<>());
            owsRequest.getRawKvp().put("viewParamsFormat", "unknown-format");
            Dispatcher.REQUEST.set(owsRequest);
            HashMap raw = new HashMap<>();
            raw.put("layers", getLayerId(BASIC_POLYGONS) + "," + getLayerId(BASIC_POLYGONS));
            raw.put("styles", "");
            raw.put("format", "image/jpeg");
            raw.put("srs", "epsg:3003");
            raw.put("bbox", "-10,-10,10,10");
            raw.put("height", "600");
            raw.put("width", "800");
            raw.put("request", "GetMap");
            raw.put("service", "wms");
            raw.put(
                    "viewParams",
                    "<VP><PS><P n=\"where\">WHERE PERSONS &gt; 1000000</P><P n=\"str\">ABCD</P></PS>"
                            + "<PS><P n=\"where\">WHERE PERSONS &gt; 10</P><P n=\"str\">FOO</P></PS></VP>");

            GetMapRequest request = reader.createRequest();
            request = reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
        } catch (ServiceException ex) {
            serviceException = ex;
        } finally {
            Dispatcher.REQUEST.set(null);
        }
        assertNotNull("ServiceException not catched", serviceException);
        assertEquals("viewParamsFormat", serviceException.getLocator());
        assertEquals(ServiceException.INVALID_PARAMETER_VALUE, serviceException.getCode());
    }

    /** Creates a HTTP embedded server with a dynamic port for testing the configures timeout. */
    private HttpServer createServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        LOG.log(
                Level.INFO,
                "Creating a mock http server at port: {0}",
                server.getAddress().getPort());
        server.createContext("/sld", createLongResponseHandler());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        server.setExecutor(threadPoolExecutor);

        return server;
    }

    private HttpHandler createLongResponseHandler() {
        HttpHandler handler = t -> {
            try {
                t.sendResponseHeaders(200, 5000000000l);
                TimeUnit.SECONDS.sleep(4);
                OutputStream outputStream = t.getResponseBody();
                outputStream.write("This is a bad style".getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (InterruptedException e) {
                LOG.log(Level.INFO, e.getMessage(), e);
            }
        };
        return handler;
    }
}
