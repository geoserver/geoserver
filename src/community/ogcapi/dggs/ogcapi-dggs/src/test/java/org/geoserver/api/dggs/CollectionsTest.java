/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class CollectionsTest extends OGCApiTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // create a H3 store and layer to be listed
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getDefaultWorkspace());
        DataStoreInfo ds = cb.buildDataStore("h3");
        ds.getConnectionParameters().put(DGGSGeometryStoreFactory.DGGS_FACTORY_ID.key, "H3");
        String nsURI = catalog.getDefaultNamespace().getURI();
        ds.getConnectionParameters().put(DGGSGeometryStoreFactory.NAMESPACE.key, nsURI);
        catalog.add(ds);

        cb.setStore(ds);
        FeatureTypeInfo ft = cb.buildFeatureType(new NameImpl(nsURI, "H3"));
        cb.setupBounds(ft);
        catalog.add(ft);
        LayerInfo li = cb.buildLayer(ft);
        catalog.add(li);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/collections", 200);
    }
}
