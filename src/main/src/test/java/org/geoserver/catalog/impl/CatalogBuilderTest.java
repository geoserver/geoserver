/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import javax.media.jai.ImageLayout;
import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.type.FeatureType;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;

public class CatalogBuilderTest extends GeoServerMockTestSupport {

    //    /**
    //     * This is a READ ONLY TEST so we can use one time setup
    //     */
    //    public static Test suite() {
    //        return new OneTimeTestSetup(new CatalogBuilderTest());
    //    }
    //
    //    @Override
    //    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
    //        super.populateDataDirectory(dataDirectory);
    //        dataDirectory.addWellKnownCoverageTypes();
    //    }

    @Override
    protected MockTestData createTestData() throws Exception {
        MockTestData testData = new MockTestData();
        testData.setIncludeRaster(true);
        return testData;
    }

    @Test
    public void testFeatureTypeNoSRS() throws Exception {
        // build a feature type (it's already in the catalog, but we just want to
        // check it's built as expected
        // LINES is a feature type with a native SRS, so we want the bounds to be there
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getDataStoreByName(MockData.BRIDGES.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.BRIDGES));

        // perform basic checks, this has no srs, so default one will be set
        assertEquals(CatalogBuilder.DEFAULT_SRS, fti.getSRS());
        assertNull(fti.getNativeCRS());
        assertNull(fti.getNativeBoundingBox());
        assertNull(fti.getLatLonBoundingBox());

        // force bounds computation
        cb.setupBounds(fti);
        assertNotNull(fti.getNativeBoundingBox());
        assertNull(fti.getNativeBoundingBox().getCoordinateReferenceSystem());
        assertNotNull(fti.getLatLonBoundingBox());
    }

    @Test
    public void testGetBoundsFromCRS() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getDataStoreByName(MockData.LINES.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.LINES));

        CoordinateReferenceSystem resourceCRS = fti.getCRS();
        assertNotNull(resourceCRS);

        // make sure the srs is as expected, otherwise the rest of the tests don't make sense
        assertEquals("EPSG:32615", fti.getSRS());
        ReferencedEnvelope crsBounds = cb.getBoundsFromCRS(fti);
        assertNotNull(crsBounds);

        CoordinateReferenceSystem exptectedCRS = CRS.decode("EPSG:32615");
        assertEquals(new ReferencedEnvelope(CRS.getEnvelope(exptectedCRS)), crsBounds);

        // if we change the srs when there's no reproject policy, should still be the same bounding
        // box
        fti.setSRS("EPSG:4326");
        fti.setProjectionPolicy(ProjectionPolicy.NONE);
        crsBounds = cb.getBoundsFromCRS(fti);
        assertEquals(new ReferencedEnvelope(CRS.getEnvelope(exptectedCRS)), crsBounds);

        // if we use reproject policy, bounds should now be different
        fti.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        crsBounds = cb.getBoundsFromCRS(fti);
        assertNotEquals(new ReferencedEnvelope(CRS.getEnvelope(exptectedCRS)), crsBounds);

        // should now be 4326 bounds
        CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326");
        assertEquals(new ReferencedEnvelope(CRS.getEnvelope(crs4326)), crsBounds);

        fti.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
        assertEquals(new ReferencedEnvelope(CRS.getEnvelope(crs4326)), crsBounds);
    }

    @Test
    public void testFindNativeCRS() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getDataStoreByName(MockData.LINES.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.LINES));
        CoordinateReferenceSystem resourceCRS = fti.getCRS();
        assertNotNull(resourceCRS);
        // make sure the srs is as expected, otherwise the rest of the tests don't make sense
        assertEquals("EPSG:32615", fti.getSRS());
        // change SRS of Resource
        fti.setNativeCRS(CRS.decode("EPSG:26713"));
        // catalog builder should return origin "EPSG:32615"
        assertEquals(CRS.lookupEpsgCode(cb.getNativeCRS(fti), false), Integer.valueOf(32615));
    }

    @Test
    public void testFeatureType() throws Exception {
        // build a feature type (it's already in the catalog, but we just want to
        // check it's built as expected
        // LINES is a feature type with a native SRS, so we want the bounds to be there
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getDataStoreByName(MockData.LINES.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.LINES));

        // perform basic checks
        assertEquals("EPSG:32615", fti.getSRS());
        assertEquals(CRS.decode("EPSG:32615", true), fti.getCRS());
        assertNull(fti.getNativeBoundingBox());
        assertNull(fti.getLatLonBoundingBox());

        // force bounds computation
        cb.setupBounds(fti);
        assertNotNull(fti.getNativeBoundingBox());
        assertNotNull(fti.getNativeBoundingBox().getCoordinateReferenceSystem());
        assertNotNull(fti.getLatLonBoundingBox());
    }

    @Test
    public void testGenericStyle() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getDataStoreByName(MockData.GENERICENTITY.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.GENERICENTITY));
        LayerInfo li = cb.buildLayer(fti);

        // check we assigned the generic style
        assertEquals("generic", li.getDefaultStyle().getName());
    }

    @Test
    public void testGeometryless() throws Exception {
        // build a feature type (it's already in the catalog, but we just want to
        // check it's built as expected
        // LINES is a feature type with a native SRS, so we want the bounds to be there
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getDataStoreByName(MockData.GEOMETRYLESS.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.GEOMETRYLESS));
        LayerInfo layer = cb.buildLayer(fti);
        cb.setupBounds(fti);

        // perform basic checks
        assertEquals(CatalogBuilder.DEFAULT_SRS, fti.getSRS());
        assertNotNull(fti.getNativeBoundingBox());
        assertTrue(fti.getNativeBoundingBox().isEmpty());
        assertNotNull(fti.getLatLonBoundingBox());
        assertFalse(fti.getLatLonBoundingBox().isEmpty());
        assertNull(layer.getDefaultStyle());
    }

    @Test
    public void testSingleBandedCoverage() throws Exception {
        // build a feature type (it's already in the catalog, but we just want to
        // check it's built as expected
        // LINES is a feature type with a native SRS, so we want the bounds to be there
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getCoverageStoreByName(MockData.TASMANIA_DEM.getLocalPart()));
        CoverageInfo ci = cb.buildCoverage();

        // perform basic checks
        assertEquals(CRS.decode("EPSG:4326", true), ci.getCRS());
        assertEquals("EPSG:4326", ci.getSRS());
        assertNotNull(ci.getNativeCRS());
        assertNotNull(ci.getNativeBoundingBox());
        assertNotNull(ci.getLatLonBoundingBox());

        // check the coverage dimensions
        List<CoverageDimensionInfo> dimensions = ci.getDimensions();
        assertEquals(1, dimensions.size());
        CoverageDimensionInfo dimension = dimensions.get(0);
        assertEquals("GRAY_INDEX", dimension.getName());
        assertEquals(1, dimension.getNullValues().size());
        assertEquals(-9999, dimension.getNullValues().get(0), 0d);
        assertEquals(Double.NEGATIVE_INFINITY, dimension.getRange().getMinimum(), 0d);
        // Huston, we have a problem here...
        // assertEquals(9999, dimension.getRange().getMaximum(), 0d);
        assertNull(dimension.getUnit());
    }

    @Test
    public void testInitCoverageSRSLookup_GEOS8973() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getCoverageStoreByName(MockData.WORLD.getLocalPart()));
        CoverageInfo cinfo = cb.buildCoverage();
        cinfo.setSRS(null);
        String wkt =
                "GEOGCS[\"ED50\",\n"
                        + "  DATUM[\"European Datum 1950\",\n"
                        + "  SPHEROID[\"International 1924\", 6378388.0, 297.0]],\n"
                        + "PRIMEM[\"Greenwich\", 0.0],\n"
                        + "UNIT[\"degree\", 0.017453292519943295]]";
        CoordinateReferenceSystem testCRS = CRS.parseWKT(wkt);
        cinfo.setNativeCRS(testCRS);
        cb.initCoverage(cinfo, "srs lookup");
        assertEquals("EPSG:4230", cinfo.getSRS());
    }

    @Test
    public void testCoverageNativeSRSLookup() throws Exception {
        Catalog cat = getCatalog();

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getCoverageStoreByName(MockData.WORLD.getLocalPart()));
        CoverageInfo cinfo = cb.buildCoverage();
        // change SRS of Resource to random
        cinfo.setNativeCRS(CRS.decode("EPSG:26713"));
        cinfo.setSRS("EPSG:26713");
        // catalog builder should return origin "EPSG:4236"
        assertEquals(CRS.lookupEpsgCode(cb.getNativeCRS(cinfo), false), Integer.valueOf(4326));
    }

    @Test
    public void testNativeBoundsDefensiveCopy() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getCoverageStoreByName(MockData.TASMANIA_DEM.getLocalPart()));
        CoverageInfo ci = cb.buildCoverage();

        // setup the reproject to declared policy, the issue happens only under this condition
        ReferencedEnvelope nativeBounds = ci.getNativeBoundingBox();
        for (ProjectionPolicy pp : ProjectionPolicy.values()) {
            ci.setProjectionPolicy(pp);
            ReferencedEnvelope bbox = ci.boundingBox();
            assertNotSame(nativeBounds, bbox);
        }
    }

    @Test
    public void testSingleBandedCoverage_GEOS7311() throws Exception {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("es", "ES"));
        testSingleBandedCoverage();
        Locale.setDefault(new Locale("fr", "FR"));
        testSingleBandedCoverage();
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testMultiBandCoverage() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getCoverageStoreByName(MockData.TASMANIA_BM.getLocalPart()));
        CoverageInfo ci = cb.buildCoverage();

        // perform basic checks
        assertEquals(CRS.decode("EPSG:4326", true), ci.getCRS());
        assertEquals("EPSG:4326", ci.getSRS());
        assertNotNull(ci.getNativeCRS());
        assertNotNull(ci.getNativeBoundingBox());
        assertNotNull(ci.getLatLonBoundingBox());

        // check the coverage dimensions
        List<CoverageDimensionInfo> dimensions = ci.getDimensions();
        assertEquals(3, dimensions.size());
        CoverageDimensionInfo dimension = dimensions.get(0);
        assertEquals("RED_BAND", dimension.getName());
        assertEquals(0, dimension.getNullValues().size());
        assertEquals(Double.NEGATIVE_INFINITY, dimension.getRange().getMinimum(), 0d);
        assertEquals(Double.POSITIVE_INFINITY, dimension.getRange().getMaximum(), 0d);
        assertEquals("W.m-2.Sr-1", dimension.getUnit());
    }

    @Test
    public void testEmptyBounds() throws Exception {
        // test the bounds of a single point
        Catalog cat = getCatalog();
        FeatureTypeInfo fti = cat.getFeatureTypeByName(toString(MockData.POINTS));
        assertEquals(
                Point.class, fti.getFeatureType().getGeometryDescriptor().getType().getBinding());
        assertEquals(1, fti.getFeatureSource(null, null).getCount(Query.ALL));

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getStoreByName(MockData.CGF_PREFIX, DataStoreInfo.class));
        FeatureTypeInfo built = cb.buildFeatureType(fti.getQualifiedName());
        cb.setupBounds(built);

        assertTrue(built.getNativeBoundingBox().getWidth() > 0);
        assertTrue(built.getNativeBoundingBox().getHeight() > 0);
    }

    @Test
    public void testEmptyLayerGroupBounds() throws Exception {
        Catalog cat = getCatalog();

        LayerGroupInfo group = cat.getFactory().createLayerGroup();
        group.setName("empty_group");

        assertNull(group.getBounds());

        // force bounds computation
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.calculateLayerGroupBounds(group);

        assertNull(group.getBounds());
    }

    @Test
    public void testLayerGroupBounds() throws Exception {
        Catalog cat = getCatalog();

        CatalogBuilder cb = new CatalogBuilder(cat);

        cb.setStore(cat.getDataStoreByName(MockData.LINES.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.LINES));
        cb.setupBounds(fti);

        LayerInfo layer = cat.getFactory().createLayer();
        layer.setResource(fti);
        layer.setName(fti.getName());
        layer.setEnabled(true);
        layer.setType(PublishedType.VECTOR);

        LayerGroupInfo group = cat.getFactory().createLayerGroup();
        group.setName("group");
        group.getLayers().add(layer);
        group.getStyles().add(null);

        assertNull(group.getBounds());

        // force bounds computation
        cb.calculateLayerGroupBounds(group);

        assertNotNull(group.getBounds());
        assertEquals(fti.getNativeBoundingBox(), group.getBounds());
    }

    @Test
    public void testLayerGroupEoBounds() throws Exception {
        Catalog cat = getCatalog();

        CatalogBuilder cb = new CatalogBuilder(cat);

        cb.setStore(cat.getDataStoreByName(MockData.LINES.getPrefix()));
        FeatureTypeInfo fti = cb.buildFeatureType(toName(MockData.LINES));
        cb.setupBounds(fti);

        LayerInfo layer = cat.getFactory().createLayer();
        layer.setResource(fti);
        layer.setName(fti.getName());
        layer.setEnabled(true);
        layer.setType(PublishedType.VECTOR);

        LayerGroupInfo group = cat.getFactory().createLayerGroup();
        group.setMode(LayerGroupInfo.Mode.EO);
        group.setName("group_EO");
        group.setRootLayer(layer);

        assertNull(group.getBounds());

        // force bounds computation
        cb.calculateLayerGroupBounds(group);

        assertNotNull(group.getBounds());
        assertEquals(fti.getNativeBoundingBox(), group.getBounds());
    }

    /** Tests we can build properly the WMS store and the WMS layer */
    @Test
    public void testWMS() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.warning("Remote OWS tests disabled, skipping catalog builder wms tests");
            return;
        }

        Catalog cat = getCatalog();

        CatalogBuilder cb = new CatalogBuilder(cat);
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL(
                RemoteOWSTestSupport.WMS_SERVER_URL
                        + "service=WMS&request=GetCapabilities&version=1.1.0");

        cb.setStore(wms);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("topp:states");
        assertWMSLayer(wmsLayer);

        LayerInfo layer = cb.buildLayer(wmsLayer);
        assertEquals(PublishedType.WMS, layer.getType());

        wmsLayer = cat.getFactory().createWMSLayer();
        wmsLayer.setName("states");
        wmsLayer.setNativeName("topp:states");
        cb.initWMSLayer(wmsLayer);
        assertWMSLayer(wmsLayer);
    }

    void assertWMSLayer(WMSLayerInfo wmsLayer) throws Exception {
        assertEquals("states", wmsLayer.getName());
        assertEquals("topp:states", wmsLayer.getNativeName());
        assertEquals("EPSG:4326", wmsLayer.getSRS());
        assertEquals("USA Population", wmsLayer.getTitle());
        assertEquals("2000 census data for United States.", wmsLayer.getAbstract());

        assertEquals(CRS.decode("EPSG:4326"), wmsLayer.getNativeCRS());
        assertNotNull(wmsLayer.getNativeBoundingBox());
        assertNotNull(wmsLayer.getLatLonBoundingBox());
        assertFalse(wmsLayer.getKeywords().isEmpty());
    }

    @Test
    public void testLookupSRSDetached() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);

        DataStoreInfo sf = cat.getDataStoreByName("sf");

        FeatureSource fs =
                sf.getDataStore(null).getFeatureSource(toName(MockData.PRIMITIVEGEOFEATURE));
        FeatureTypeInfo ft = cat.getFactory().createFeatureType();
        ft.setNativeName("PrimitiveGeoFeature");
        assertNull(ft.getSRS());
        assertNull(ft.getCRS());

        cb.lookupSRS(ft, fs, true);

        assertNotNull(ft.getSRS());
        assertNotNull(ft.getCRS());
    }

    @Test
    public void testSetupBoundsDetached() throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);

        DataStoreInfo sf = cat.getDataStoreByName("sf");

        FeatureSource fs =
                sf.getDataStore(null).getFeatureSource(toName(MockData.PRIMITIVEGEOFEATURE));
        FeatureTypeInfo ft = cat.getFactory().createFeatureType();
        ft.setNativeName("PrimitiveGeoFeature");
        assertNull(ft.getNativeBoundingBox());
        assertNull(ft.getLatLonBoundingBox());

        cb.lookupSRS(ft, fs, true);
        cb.setupBounds(ft, fs);

        assertNotNull(ft.getNativeBoundingBox());
        assertNotNull(ft.getLatLonBoundingBox());
    }

    @Test
    public void testMetadataFromFeatueSource() throws Exception {
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        cb.setStore(cb.buildDataStore("fooStore"));

        FeatureType ft = createMock(FeatureType.class);
        expect(ft.getName()).andReturn(new NameImpl("foo")).anyTimes();
        expect(ft.getCoordinateReferenceSystem()).andReturn(null).anyTimes();
        expect(ft.getGeometryDescriptor()).andReturn(null).anyTimes();
        replay(ft);

        ResourceInfo rInfo = createMock(ResourceInfo.class);
        expect(rInfo.getTitle()).andReturn("foo title");
        expect(rInfo.getDescription()).andReturn("foo description");
        expect(rInfo.getKeywords())
                .andReturn(new LinkedHashSet<String>(Arrays.asList("foo", "bar", "baz", "")))
                .anyTimes();
        replay(rInfo);

        FeatureSource fs = createMock(FeatureSource.class);
        expect(fs.getSchema()).andReturn(ft).anyTimes();
        expect(fs.getInfo()).andReturn(rInfo).anyTimes();
        expect(fs.getName()).andReturn(ft.getName()).anyTimes();
        replay(fs);

        FeatureTypeInfo ftInfo = cb.buildFeatureType(fs);
        assertEquals("foo title", ftInfo.getTitle());
        assertEquals("foo description", ftInfo.getDescription());
        assertTrue(ftInfo.getKeywords().contains(new Keyword("foo")));
        assertTrue(ftInfo.getKeywords().contains(new Keyword("bar")));
        assertTrue(ftInfo.getKeywords().contains(new Keyword("baz")));
    }

    @Test
    public void testSetupMetadataResourceInfoException() throws Exception {
        FeatureTypeInfo ftInfo = createMock(FeatureTypeInfo.class);
        expect(ftInfo.getTitle()).andReturn("foo");
        expect(ftInfo.getDescription()).andReturn("foo");
        expect(ftInfo.getKeywords()).andReturn(null);
        replay(ftInfo);

        FeatureSource fs = createMock(FeatureSource.class);
        expect(fs.getInfo()).andThrow(new UnsupportedOperationException());
        replay(fs);

        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        cb.setupMetadata(ftInfo, fs);
    }

    @Test
    public void testLatLonBounds() throws Exception {
        ReferencedEnvelope nativeBounds =
                new ReferencedEnvelope(700000, 800000, 4000000, 4100000, null);
        CoordinateReferenceSystem crs = CRS.decode("EPSG:32632", true);
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        ReferencedEnvelope re = cb.getLatLonBounds(nativeBounds, crs);
        assertEquals(DefaultGeographicCRS.WGS84, re.getCoordinateReferenceSystem());
        assertEquals(11.22, re.getMinX(), 0.01);
        assertEquals(36.1, re.getMinY(), 0.01);
    }

    @Test
    public void testWMSLayer111() throws Exception {
        TestHttpClientProvider.startTest();
        try {
            String baseURL = TestHttpClientProvider.MOCKSERVER + "/wms11";
            MockHttpClient client = new MockHttpClient();
            URL capsURL = new URL(baseURL + "?service=WMS&request=GetCapabilities&version=1.1.0");
            client.expectGet(
                    capsURL,
                    new MockHttpResponse(getClass().getResource("caps111.xml"), "text/xml"));
            TestHttpClientProvider.bind(client, capsURL);

            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            WMSStoreInfo store = cb.buildWMSStore("test-store");
            store.setCapabilitiesURL(capsURL.toExternalForm());
            cb.setStore(store);
            WMSLayerInfo layer = cb.buildWMSLayer("world4326");
            layer.reset();
            // check the bbox has the proper axis order
            assertEquals("EPSG:4326", layer.getSRS());
            ReferencedEnvelope bbox = layer.getLatLonBoundingBox();
            assertEquals(-180, bbox.getMinX(), 0d);
            assertEquals(-90, bbox.getMinY(), 0d);
            assertEquals(180, bbox.getMaxX(), 0d);
            assertEquals(90, bbox.getMaxY(), 0d);
            assertTrue(!layer.availableFormats().isEmpty());
            assertTrue(layer.getStyles().isEmpty());
            assertTrue(layer.getDefaultStyle().getName().isEmpty());

        } finally {
            TestHttpClientProvider.endTest();
        }
    }

    @Test
    public void testWMSLayer130() throws Exception {
        TestHttpClientProvider.startTest();
        try {
            String baseURL = TestHttpClientProvider.MOCKSERVER + "/wms13";
            MockHttpClient client = new MockHttpClient();
            URL capsURL = new URL(baseURL + "?service=WMS&request=GetCapabilities&version=1.3.0");
            client.expectGet(
                    capsURL,
                    new MockHttpResponse(getClass().getResource("caps130.xml"), "text/xml"));
            TestHttpClientProvider.bind(client, capsURL);

            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            WMSStoreInfo store = cb.buildWMSStore("test-store");
            store.setCapabilitiesURL(capsURL.toExternalForm());
            cb.setStore(store);
            WMSLayerInfo layer = cb.buildWMSLayer("world4326");

            // check the bbox has the proper axis order
            assertEquals("EPSG:4326", layer.getSRS());
            ReferencedEnvelope bbox = layer.getLatLonBoundingBox();
            assertEquals(-180, bbox.getMinX(), 0d);
            assertEquals(-90, bbox.getMinY(), 0d);
            assertEquals(180, bbox.getMaxX(), 0d);
            assertEquals(90, bbox.getMaxY(), 0d);
            assertTrue(!layer.availableFormats().isEmpty());
            assertTrue(layer.getStyles().isEmpty());
            assertTrue(layer.getDefaultStyle().getName().isEmpty());
        } finally {
            TestHttpClientProvider.endTest();
        }
    }

    @Test
    public void testWMSLayer130crs84() throws Exception {
        TestHttpClientProvider.startTest();
        try {
            String baseURL = TestHttpClientProvider.MOCKSERVER + "/wms13";
            MockHttpClient client = new MockHttpClient();
            URL capsURL = new URL(baseURL + "?service=WMS&request=GetCapabilities&version=1.3.0");
            client.expectGet(
                    capsURL,
                    new MockHttpResponse(getClass().getResource("caps130_crs84.xml"), "text/xml"));
            TestHttpClientProvider.bind(client, capsURL);

            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            WMSStoreInfo store = cb.buildWMSStore("test-store");
            store.setCapabilitiesURL(capsURL.toExternalForm());
            cb.setStore(store);
            WMSLayerInfo layer = cb.buildWMSLayer("world4326");

            // check the bbox has the proper axis order
            assertEquals("EPSG:4326", layer.getSRS());
            ReferencedEnvelope bbox = layer.getLatLonBoundingBox();
            assertEquals(-180, bbox.getMinX(), 0d);
            assertEquals(-90, bbox.getMinY(), 0d);
            assertEquals(180, bbox.getMaxX(), 0d);
            assertEquals(90, bbox.getMaxY(), 0d);
            assertTrue(!layer.availableFormats().isEmpty());
            assertTrue(layer.getStyles().isEmpty());
            assertTrue(layer.getDefaultStyle().getName().isEmpty());
        } finally {
            TestHttpClientProvider.endTest();
        }
    }

    @Test
    public void testWMTSLayer100() throws Exception {
        TestHttpClientProvider.startTest();
        try {
            String baseURL = TestHttpClientProvider.MOCKSERVER + "/wmts100";
            MockHttpClient client = new MockHttpClient();
            URL capsURL = new URL(baseURL + "?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
            client.expectGet(
                    capsURL,
                    new MockHttpResponse(getClass().getResource("nasa.getcapa.xml"), "text/xml"));
            TestHttpClientProvider.bind(client, capsURL);

            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            WMTSStoreInfo store = cb.buildWMTSStore("test-wmts-store");
            store.setCapabilitiesURL(capsURL.toExternalForm());
            cb.setStore(store);
            WMTSLayerInfo layer = cb.buildWMTSLayer("AMSR2_Wind_Speed_Night");

            // check the bbox has the proper axis order
            assertEquals("Wind Speed (Night, AMSR2, GCOM-W1)", layer.getTitle());
            assertEquals("EPSG:4326", layer.getSRS());

            ReferencedEnvelope bbox = layer.getLatLonBoundingBox();
            assertEquals(-180, bbox.getMinX(), 0d);
            assertEquals(-90, bbox.getMinY(), 0d);
            assertEquals(180, bbox.getMaxX(), 0d);
            assertEquals(90, bbox.getMaxY(), 0d);
        } finally {
            TestHttpClientProvider.endTest();
        }
    }

    @Test
    public void testWgs84BoundsFromCompoundCRS() throws Exception {
        try {
            MapProjection.SKIP_SANITY_CHECKS = true;
            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            ReferencedEnvelope3D bounds =
                    new ReferencedEnvelope3D(
                            142892, 470783, 16, 142900, 470790, 20, CRS.decode("EPSG:7415"));
            // used to throw an exception here
            ReferencedEnvelope latLonBounds =
                    cb.getLatLonBounds(bounds, bounds.getCoordinateReferenceSystem());
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            CRS.decode("EPSG:4326"), latLonBounds.getCoordinateReferenceSystem()));
            // System.out.println(latLonBounds);
        } finally {
            MapProjection.SKIP_SANITY_CHECKS = false;
        }
    }

    @Test
    public void testSetupCoverageOnEmptyRead() throws Exception {
        // fake coverage info
        ReferencedEnvelope envelope = MockTestData.DEFAULT_LATLON_ENVELOPE;
        BufferedImage bi = new BufferedImage(360, 180, BufferedImage.TYPE_3BYTE_BGR);
        ImageLayout layout = new ImageLayout(bi);
        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, 360, 180);
        GridToEnvelopeMapper mapper = new GridToEnvelopeMapper(gridRange, envelope);
        AffineTransform gridToWorld = mapper.createAffineTransform();

        final String rasterSource = "http://www.geoserver.org/foo";
        // setup the format and the reader
        AbstractGridCoverage2DReader reader = createMock(AbstractGridCoverage2DReader.class);
        AbstractGridFormat format = createMock(AbstractGridFormat.class);

        expect(reader.getOriginalEnvelope()).andReturn(new GeneralEnvelope(envelope)).anyTimes();
        expect(reader.getCoordinateReferenceSystem())
                .andReturn(envelope.getCoordinateReferenceSystem())
                .anyTimes();
        expect(reader.getOriginalGridRange()).andReturn(gridRange).anyTimes();
        expect(reader.getImageLayout()).andReturn(layout).anyTimes();
        expect(reader.getFormat()).andReturn(format).anyTimes();
        expect(reader.getGridCoverageCount()).andReturn(1);
        expect(reader.getOriginalGridToWorld(EasyMock.anyObject(PixelInCell.class)))
                .andReturn(new AffineTransform2D(gridToWorld))
                .anyTimes();
        expect(reader.read(EasyMock.anyObject(GeneralParameterValue[].class))).andReturn(null);
        expect(reader.getGridCoverageNames()).andReturn(new String[] {"TheCoverage"});
        replay(reader);
        expect(format.getReader(EasyMock.eq(rasterSource), EasyMock.anyObject(Hints.class)))
                .andReturn(reader)
                .anyTimes();
        expect(format.getName()).andReturn("TheFormat").anyTimes();
        expect(format.getReadParameters())
                .andReturn(new CustomFormat().getReadParameters())
                .anyTimes();
        replay(format);

        CoverageStoreInfo csi = createMock(CoverageStoreInfo.class);
        expect(csi.getURL()).andReturn(rasterSource).anyTimes();
        expect(csi.getFormat()).andReturn(format).anyTimes();
        expect(csi.getId()).andReturn("ThisIsMe").anyTimes();
        expect(csi.getName()).andReturn("ThisIsMe").anyTimes();
        expect(csi.getWorkspace()).andReturn(getCatalog().getDefaultWorkspace()).anyTimes();
        replay(csi);

        // setup a non cloning resource pool and catalog
        Catalog cat = new CatalogImpl();
        ResourcePool rp =
                new ResourcePool(cat) {
                    public CoverageStoreInfo clone(
                            CoverageStoreInfo source, boolean allowEnvParametrization) {
                        return source;
                    };
                };
        cat.setResourcePool(rp);

        // make it build the coverage info without access to a grid coverage 2d
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(csi);
        CoverageInfo ci = cb.buildCoverage();

        assertEquals("TheCoverage", ci.getName());
        List<CoverageDimensionInfo> dimensions = ci.getDimensions();
        assertEquals(3, dimensions.size());
        assertEquals("RED_BAND", dimensions.get(0).getName());
        assertEquals("GREEN_BAND", dimensions.get(1).getName());
        assertEquals("BLUE_BAND", dimensions.get(2).getName());
    }
}
