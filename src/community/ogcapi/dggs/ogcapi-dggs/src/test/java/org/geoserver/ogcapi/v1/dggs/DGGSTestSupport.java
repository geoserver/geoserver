/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;
import org.geotools.feature.NameImpl;

public class DGGSTestSupport extends OGCApiTestSupport {

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
        DimensionInfoImpl time = new DimensionInfoImpl();
        time.setAttribute("date");
        ft.getMetadata().put(ResourceInfo.TIME, time);
        catalog.add(ft);
        LayerInfo li = cb.buildLayer(ft);
        catalog.add(li);

        // disable feature bounding
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setFeatureBounding(false);
        gs.save(wfs);
    }
}
