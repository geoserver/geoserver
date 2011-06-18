/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;

public class SQLViewNewPage extends SQLViewAbstractPage {

    public SQLViewNewPage(PageParameters params) throws IOException {
        super(params);
    }

    @Override
    protected void onSave() {
        try {
            VirtualTable vt = buildVirtualTable();
            DataStoreInfo dsInfo = getCatalog().getStore(storeId, DataStoreInfo.class);
            JDBCDataStore ds = (JDBCDataStore) dsInfo.getDataStore(null);
            ds.addVirtualTable(vt);

            CatalogBuilder builder = new CatalogBuilder(getCatalog());
            builder.setStore(dsInfo);
            FeatureTypeInfo fti = builder.buildFeatureType(ds.getFeatureSource(vt.getName()));
            fti.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
            LayerInfo layerInfo = builder.buildLayer(fti);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                    .getString());
        }
    }
    
    protected void onCancel() {
        setResponsePage(LayerPage.class);
    }

}
