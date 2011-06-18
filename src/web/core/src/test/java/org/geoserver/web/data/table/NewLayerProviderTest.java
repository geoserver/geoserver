package org.geoserver.web.data.table;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPageProvider;


public class NewLayerProviderTest extends GeoServerWicketTestSupport {
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory)
            throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWellKnownCoverageTypes();
    }
    
    public void testFeatureType() {
        StoreInfo cite = getCatalog().getStoreByName( MockData.CITE_PREFIX,StoreInfo.class );
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(cite.getId());
        provider.setShowPublished(true);
        assertTrue(provider.size() > 0);
        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }
    
    public void testCoverages() {
        StoreInfo dem = getCatalog().getStoreByName( MockData.TASMANIA_DEM.getLocalPart(),StoreInfo.class );
        NewLayerPageProvider provider = new NewLayerPageProvider();
        provider.setStoreId(dem.getId());
        provider.setShowPublished(true);
        assertTrue(provider.size() > 0);
        provider.setShowPublished(false);
        assertEquals(0, provider.size());
    }
    
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
