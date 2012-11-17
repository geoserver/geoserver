/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.geom.Point;

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

        // perform basic checks, this has no srs so no lat/lon bbox computation possible
        assertNull(fti.getSRS());
        assertNull(fti.getNativeCRS());
        assertNull(fti.getNativeBoundingBox());
        assertNull(fti.getLatLonBoundingBox());

        // force bounds computation
        cb.setupBounds(fti);
        assertNotNull(fti.getNativeBoundingBox());
        assertNull(fti.getNativeBoundingBox().getCoordinateReferenceSystem());
        assertNull(fti.getLatLonBoundingBox());
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
        assertNull(fti.getCRS());
        // ... not so sure about this one, null would seem more natural
        assertTrue(fti.getNativeBoundingBox().isEmpty());
        assertNull(fti.getLatLonBoundingBox());
        assertNull(layer.getDefaultStyle());
    }

    @Test
    public void testCoverage() throws Exception {
        // build a feature type (it's already in the catalog, but we just want to
        // check it's built as expected
        // LINES is a feature type with a native SRS, so we want the bounds to be there
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getCoverageStoreByName(MockData.TASMANIA_DEM.getLocalPart()));
        CoverageInfo fti = cb.buildCoverage();

        // perform basic checks
        assertEquals(CRS.decode("EPSG:4326", true), fti.getCRS());
        assertEquals("EPSG:4326", fti.getSRS());
        assertNotNull(fti.getNativeCRS());
        assertNotNull(fti.getNativeBoundingBox());
        assertNotNull(fti.getLatLonBoundingBox());
    }

    @Test
    public void testEmptyBounds() throws Exception {
        // test the bounds of a single point
        Catalog cat = getCatalog();
        FeatureTypeInfo fti = cat.getFeatureTypeByName(toString(MockData.POINTS));
        assertEquals(Point.class, fti.getFeatureType().getGeometryDescriptor().getType()
                .getBinding());
        assertEquals(1, fti.getFeatureSource(null, null).getCount(Query.ALL));

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(cat.getStoreByName(MockData.CGF_PREFIX, DataStoreInfo.class));
        FeatureTypeInfo built = cb.buildFeatureType(fti.getQualifiedName());
        cb.setupBounds(built);

        assertTrue(built.getNativeBoundingBox().getWidth() > 0);
        assertTrue(built.getNativeBoundingBox().getHeight() > 0);
    }

    /**
     * Tests we can build properly the WMS store and the WMS layer
     * 
     * @throws Exception
     */
    @Test
    public void testWMS() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.warning("Remote OWS tests disabled, skipping catalog builder wms tests");
            return;
        }

        Catalog cat = getCatalog();

        CatalogBuilder cb = new CatalogBuilder(cat);
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL(RemoteOWSTestSupport.WMS_SERVER_URL
                + "service=WMS&request=GetCapabilities");
        cat.save(wms);

        cb.setStore(wms);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("topp:states");
        assertWMSLayer(wmsLayer);
        
        LayerInfo layer = cb.buildLayer(wmsLayer);
        assertEquals(LayerInfo.Type.WMS, layer.getType());

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
        assertEquals("This is some census data on the states.", wmsLayer.getAbstract());
        
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
        expect(rInfo.getKeywords()).andReturn(
            new LinkedHashSet<String>(Arrays.asList("foo", "bar", "baz", ""))).anyTimes();
        replay(rInfo);
        
        FeatureSource fs = createMock(FeatureSource.class);
        expect(fs.getSchema()).andReturn(ft).anyTimes();
        expect(fs.getInfo()).andReturn(rInfo).anyTimes();
        replay(fs);
            
        FeatureTypeInfo ftInfo = cb.buildFeatureType(fs);
        assertEquals("foo title", ftInfo.getTitle());
        assertEquals("foo description", ftInfo.getDescription());
        assertTrue(ftInfo.getKeywords().contains(new Keyword("foo")));
        assertTrue(ftInfo.getKeywords().contains(new Keyword("bar")));
        assertTrue(ftInfo.getKeywords().contains(new Keyword("baz")));
    }

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
}
