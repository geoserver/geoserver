/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.table;

import static org.junit.Assert.*;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPageProvider;
import org.junit.Test;

public class NewLayerProviderTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testFeatureType() {
        StoreInfo cite = getCatalog().getStoreByName(MockData.CITE_PREFIX, StoreInfo.class);
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());
        provider.setShowPublished(true);
        assertTrue(provider.size() > 0);
        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }

    @Test
    public void testCoverages() {
        StoreInfo dem =
                getCatalog().getStoreByName(MockData.TASMANIA_DEM.getLocalPart(), StoreInfo.class);
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(dem.getId());
        provider.setShowPublished(true);
        assertTrue(provider.size() > 0);
        provider.setShowPublished(false);
        // todo: fix this
        // assertEquals(0, provider.size());
    }

    @Test
    public void testEmpty() {
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setShowPublished(true);
        assertEquals(0, provider.size());
        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }

    /**
     * As per GEOS-3120, if a resource is published but it's name changed, it should still show up
     * as published. It wasn't being the case due to comparing the resource's name instead of the
     * nativeName against the name the DataStore provides
     */
    @Test
    public void testPublishedUnpublishedWithChangedResourceName() {
        Catalog catalog = getCatalog();
        StoreInfo cite = catalog.getStoreByName(MockData.CITE_PREFIX, StoreInfo.class);

        List<FeatureTypeInfo> resources = catalog.getResourcesByStore(cite, FeatureTypeInfo.class);
        assertTrue(resources.size() > 0);

        final int numberOfPublishedResources = resources.size();

        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());
        provider.setShowPublished(false);
        assertEquals(0, provider.size());

        provider.setShowPublished(true);
        assertEquals(numberOfPublishedResources, provider.size());

        FeatureTypeInfo typeInfo = resources.get(0);
        typeInfo.setName("notTheNativeName");
        catalog.save(typeInfo);

        provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());

        provider.setShowPublished(true);
        assertEquals(numberOfPublishedResources, provider.size());

        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }
}
