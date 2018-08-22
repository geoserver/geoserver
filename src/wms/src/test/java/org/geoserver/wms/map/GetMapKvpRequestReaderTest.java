/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import junit.framework.Test;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.kvp.URLKvpParser;
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
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Style;
import org.geotools.util.DateRange;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

@SuppressWarnings("unchecked")
public class GetMapKvpRequestReaderTest extends KvpRequestReaderTestSupport {
    GetMapKvpRequestReader reader;

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
        return new OneTimeTestSetup(new GetMapKvpRequestReaderTest());
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
        reader = new GetMapKvpRequestReader(wms);
    }

    public void testSldEntityResolver() throws Exception {
        WMS wms = new WMS(getGeoServer());
        GeoServerInfo geoserverInfo = wms.getGeoServer().getGlobal();
        try {
            // enable entities in external SLD files
            geoserverInfo.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(geoserverInfo);

            // test setting has been saved
            assertNotNull(wms.getGeoServer().getGlobal().isXmlExternalEntitiesEnabled());
            assertTrue((Boolean) wms.getGeoServer().getGlobal().isXmlExternalEntitiesEnabled());

            // test no custom entity resolver will be used
            GetMapKvpRequestReader reader = new GetMapKvpRequestReader(wms);
            assertNull(reader.entityResolverProvider.getEntityResolver());

            // disable entities
            geoserverInfo.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(geoserverInfo);

            // since XML entities are disabled for external SLD files
            // I need an entity resolver which enforce this
            reader = new GetMapKvpRequestReader(wms);
            assertNotNull(reader.entityResolverProvider.getEntityResolver());

            // try default value: entities should be disabled
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);

            reader = new GetMapKvpRequestReader(wms);
            assertNotNull(reader.entityResolverProvider.getEntityResolver());
        } finally {
            // reset to default
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);
        }
    }

    public void testCreateRequest() throws Exception {
        GetMapRequest request = (GetMapRequest) reader.createRequest();
        assertNotNull(request);
    }

    public void testReadMandatory() throws Exception {
        HashMap raw = new HashMap();
        raw.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        raw.put("styles", MockData.BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        String layer = MockData.BASIC_POLYGONS.getLocalPart();
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

    public void testReadOptional() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("bgcolor", "000000");
        kvp.put("transparent", "true");
        kvp.put("tiled", "true");
        kvp.put("tilesorigin", "1.2,3.4");
        kvp.put("buffer", "1");
        kvp.put("palette", "SAFE");
        kvp.put("time", "2006-02-27T22:08:12Z");
        kvp.put("elevation", "4");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

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

    public void testDefaultStyle() throws Exception {
        HashMap raw = new HashMap();
        raw.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix()
                        + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart()
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

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
        assertEquals(2, request.getStyles().size());
        LayerInfo basicPolygons =
                getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        LayerInfo buildings = getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart());
        assertEquals(basicPolygons.getDefaultStyle().getStyle(), request.getStyles().get(0));
        assertEquals(buildings.getDefaultStyle().getStyle(), request.getStyles().get(1));
    }

    public void testInterpolations() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("layers", getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("interpolations", "bicubic");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(1, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertTrue(request.getInterpolations().get(0) instanceof InterpolationBicubic);

        kvp.put(
                "layers",
                getLayerId(MockData.BASIC_POLYGONS)
                        + ","
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + ","
                        + getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("interpolations", "bicubic,,bilinear");
        request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(3, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertNull(request.getInterpolations().get(1));
        assertNotNull(request.getInterpolations().get(2));
        assertTrue(request.getInterpolations().get(2) instanceof InterpolationBilinear);
    }

    public void testInterpolationsForLayerGroups() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("layers", "testGroup2");
        kvp.put("interpolations", "bicubic");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(2, request.getInterpolations().size());
        assertNotNull(request.getInterpolations().get(0));
        assertTrue(request.getInterpolations().get(0) instanceof InterpolationBicubic);

        assertNotNull(request.getInterpolations().get(1));
        assertTrue(request.getInterpolations().get(1) instanceof InterpolationBicubic);

        kvp.put("layers", "testGroup2,testGroup," + getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("interpolations", "bicubic,bilinear,nearest neighbor");

        request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

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

        kvp.put("layers", "testGroup2,testGroup," + getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("interpolations", ",bilinear");

        request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getInterpolations());
        assertEquals(4, request.getInterpolations().size());
        assertNull(request.getInterpolations().get(0));
        assertNull(request.getInterpolations().get(1));
        assertNotNull(request.getInterpolations().get(2));
        assertTrue(request.getInterpolations().get(2) instanceof InterpolationBilinear);
        assertNull(request.getInterpolations().get(3));
    }

    public void testFilter() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("layers", getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("filter", "<Filter><FeatureId id=\"foo\"/></Filter>");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getFilter());
        assertEquals(1, request.getFilter().size());

        Id fid = (Id) request.getFilter().get(0);
        assertEquals(1, fid.getIDs().size());

        assertEquals("foo", fid.getIDs().iterator().next());
    }

    public void testCQLFilter() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("cql_filter", "foo = bar");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getCQLFilter());
        assertEquals(1, request.getCQLFilter().size());

        PropertyIsEqualTo filter = (PropertyIsEqualTo) request.getCQLFilter().get(0);
    }

    public void testFeatureId() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("featureid", "foo");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getFeatureId());
        assertEquals(1, request.getFeatureId().size());

        assertEquals("foo", request.getFeatureId().get(0));
    }

    public void testSortBy() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("layers", getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("sortBy", "FID D");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertEquals(1, request.getSortBy().size());

        List<SortBy> sort = request.getSortBy().get(0);
        assertEquals(1, sort.size());
        assertSortBy(sort.get(0), "FID", SortOrder.DESCENDING);
    }

    public void testSortByGroup() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("layers", "testGroup2");
        kvp.put("sortBy", "FID D");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertEquals(2, request.getSortBy().size());

        List<SortBy> sort1 = request.getSortBy().get(0);
        assertEquals(1, sort1.size());
        assertSortBy(sort1.get(0), "FID", SortOrder.DESCENDING);

        List<SortBy> sort2 = request.getSortBy().get(0);
        assertEquals(1, sort2.size());
        assertSortBy(sort2.get(0), "FID", SortOrder.DESCENDING);
    }

    public void testSortByLessThanRequired() throws Exception {
        HashMap<String, Serializable> kvp = new HashMap<>();
        kvp.put("layers", getLayerId(MockData.BASIC_POLYGONS) + "," + getLayerId(MockData.LAKES));
        kvp.put("sortBy", "FID D");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
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

    public void testSldNoDefault() throws Exception {
        // no style name, no default, we should fall back on the server default
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        // the kvp should be already in decoded form
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        reader.setLaxStyleMatchAllowed(false);
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getSld());
        assertEquals(URLKvpParser.fixURL(decoded), request.getSld().toExternalForm());
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("BasicPolygons", style.getName());
    }

    public void testSldDefault() throws Exception {
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URLKvpParser.fixURL(decoded), request.getSld().toExternalForm());
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
    }

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

        MockHttpClientConnectionManager manager =
                new MockHttpClientConnectionManager(builder.toString(), true);
        reader = new GetMapKvpRequestReader(wms, manager);
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap();
        String url = "http://cached_sld";
        kvp.put("sld", url);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(manager.getConnections(), 1);

        request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        // no connection is done, the result is taken from cache
        assertEquals(manager.getConnections(), 1);
    }

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

        MockHttpClientConnectionManager manager =
                new MockHttpClientConnectionManager(builder.toString(), false);
        reader = new GetMapKvpRequestReader(wms, manager);
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap();
        String url = "http://cached_sld";
        kvp.put("sld", url);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(manager.getConnections(), 1);

        request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        // new connection is done, the result is NOT taken from cache
        assertEquals(manager.getConnections(), 2);
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
        reader = new GetMapKvpRequestReader(wms);
        GetMapRequest request = (GetMapRequest) reader.createRequest();
        boolean error = false;
        try {
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);
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
        reader = new GetMapKvpRequestReader(wms);
        GetMapRequest request = (GetMapRequest) reader.createRequest();
        boolean error = false;
        try {
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);
        } catch (ServiceException e) {
            error = true;
        }
        getGeoServer().remove(info);
        getGeoServer().add(oldInfo);
        assertTrue(error);
    }

    public void testSldNamed() throws Exception {
        // style name matching one in the sld
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "TheLibraryModeStyle");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URLKvpParser.fixURL(decoded), request.getSld().toExternalForm());
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
    }

    public void testSldFailLookup() throws Exception {
        // nothing matches the required style name
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            // System.out.println(e);
        }
    }

    public void testSldConnectionFailure() throws Exception {
        // Connection for specified external SLD fails while retrieving SLD
        HashMap kvp = new HashMap();

        URL url = new URL("http://hostthatdoesnotexist/");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertTrue("Exception should not reveal its cause", e.getCause() == null);
        }
    }

    public void testSldNotExist() throws Exception {
        // Specified external SLD does not exist
        HashMap kvp = new HashMap();

        URL url = new URL(GetMapKvpRequestReaderTest.class.getResource(""), "does-not-exist");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertTrue("Exception should not reveal its cause", e.getCause() == null);
        }
    }

    public void testSldNotXML() throws Exception {
        // Specified external SLD is not XML
        HashMap kvp = new HashMap();

        URL url = GetMapKvpRequestReaderTest.class.getResource("paletted.tif");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertTrue("Exception should not reveal its cause", e.getCause() == null);
        }
    }

    public void testSldNotSld() throws Exception {
        // Specified external SLD is XML that is not SLD
        HashMap kvp = new HashMap();

        URL url =
                GetMapKvpRequestReaderTest.class.getResource(
                        "WMSPostLayerGroupNonDefaultStyle.xml");

        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            assertTrue("Exception should not reveal its cause", e.getCause() == null);
        }
    }

    public void testSldFeatureTypeConstraints() throws Exception {
        // no styles, no layer, the full definition is in the sld
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsFeatureTypeConstaint.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URLKvpParser.fixURL(decoded), request.getSld().toExternalForm());
        // check the style
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
        // check the layer
        assertEquals(1, request.getLayers().size());
        MapLayerInfo layer = request.getLayers().get(0);
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), layer.getName());
        // check the filter imposed in the feature type constraint
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        assertEquals(1, layer.getLayerFeatureConstraints().length);
        assertEquals(
                ff.equals(ff.property("ID"), ff.literal("xyz")),
                layer.getLayerFeatureConstraints()[0].getFilter());
    }

    public void testSldLibraryFeatureTypeConstraints() throws Exception {
        // no styles, no layer, the full definition is in the sld
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsFeatureTypeConstaint.sld");
        String decoded = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        kvp.put("sld", decoded);
        kvp.put(
                "layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "TheLibraryModeStyle");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(URLKvpParser.fixURL(decoded), request.getSld().toExternalForm());
        // check the style
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
        // check the layer
        assertEquals(1, request.getLayers().size());
        MapLayerInfo layer = request.getLayers().get(0);
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), layer.getName());
        // check the filter imposed in the feature type constraint
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        assertEquals(1, layer.getLayerFeatureConstraints().length);
        assertEquals(
                ff.equals(ff.property("ID"), ff.literal("xyz")),
                layer.getLayerFeatureConstraints()[0].getFilter());
    }

    /**
     * One of the cite tests ensures that WMTVER is recognized as VERSION and the server does not
     * complain
     */
    public void testWmtVer() throws Exception {
        dispatcher.setCiteCompliant(true);
        String request =
                "wms?SERVICE=WMS&&WiDtH=200&FoRmAt=image/png&LaYeRs=cite:Lakes&StYlEs=&BbOx=0,-0.0020,0.0040,0&ReQuEsT=GetMap&HeIgHt=100&SrS=EPSG:4326&WmTvEr=1.1.1";
        assertEquals("image/png", getAsServletResponse(request).getContentType());
    }

    public void testRemoteWFS() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        HashMap raw = new HashMap();
        raw.put("layers", "topp:states");
        raw.put("styles", MockData.BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", RemoteOWSTestSupport.WFS_SERVER_URL);

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        assertEquals("WFS", request.getRemoteOwsType()); // TODO: handle case?
        assertEquals(new URL(RemoteOWSTestSupport.WFS_SERVER_URL), request.getRemoteOwsURL());
        assertEquals(1, request.getLayers().size());
        assertEquals(
                PublishedType.REMOTE.getCode().intValue(), request.getLayers().get(0).getType());
        assertEquals("topp:states", request.getLayers().get(0).getName());
    }

    public void testRemoteWFSNoStyle() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        HashMap raw = new HashMap();
        raw.put("layers", "topp:states");
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", RemoteOWSTestSupport.WFS_SERVER_URL);

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            fail("This should have thrown an exception because of the missing style");
        } catch (ServiceException e) {
            assertEquals("NoDefaultStyle", e.getCode());
        }
    }

    public void testRemoteWFSInvalidURL() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        HashMap raw = new HashMap();
        raw.put("layers", "topp:states");
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", "http://phantom.openplans.org:8080/crapserver/wfs?");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            fail("This should have thrown an exception because of the non existent layer");
        } catch (ServiceException e) {
            e.printStackTrace();
            assertEquals("RemoteOWSFailure", e.getCode());
        }
    }

    public void testGroupInSLD() throws Exception {
        // see GEOS-1818
        final HashMap kvp = new HashMap();
        kvp.put("srs", "epsg:4326");
        kvp.put(
                "bbox",
                "124.38035938267053,-58.45445933799711,169.29632161732948,-24.767487662002893");
        kvp.put("width", "640");
        kvp.put("height", "480");
        kvp.put("format", "image/png");
        final URL url = GetMapKvpRequestReader.class.getResource("BaseMapGroup.sld");
        // URLDecoder.decode fixes GEOS-3709
        kvp.put("sld", URLDecoder.decode(url.toString(), "UTF-8"));
        kvp.put("version", "1.1.1");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertEquals(1, request.getLayers().size());
        assertEquals(1, request.getStyles().size());
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), request.getLayers().get(0).getName());
        Style expectedStyle = getCatalog().getStyleByName("polygon").getStyle();
        assertEquals(expectedStyle, request.getStyles().get(0));
    }

    public void testViewParams() throws Exception {
        HashMap raw = new HashMap();
        raw.put("layers", getLayerId(MockData.BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(1, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
    }

    public void testMultipleViewParams() throws Exception {
        HashMap raw = new HashMap();
        raw.put(
                "layers",
                getLayerId(MockData.BASIC_POLYGONS) + "," + getLayerId(MockData.BASIC_POLYGONS));
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
                "where:WHERE PERSONS > 1000000;str:ABCD,where:WHERE PERSONS > 10;str:FOO");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(2, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
        viewParams = viewParamsList.get(1);
        assertEquals("WHERE PERSONS > 10", viewParams.get("where"));
        assertEquals("FOO", viewParams.get("str"));
    }

    public void testFanOutViewParams() throws Exception {
        HashMap raw = new HashMap();
        raw.put(
                "layers",
                getLayerId(MockData.BASIC_POLYGONS) + "," + getLayerId(MockData.BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(2, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
        viewParams = viewParamsList.get(1);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
    }
}
