/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;

public class SQLViewNewPage extends SQLViewAbstractPage {

    /** serialVersionUID */
    private static final long serialVersionUID = 3670565306101168775L;

    public SQLViewNewPage(PageParameters params) throws IOException {
        super(params);
    }

    @Override
    protected void onSave() {
        try {
            VirtualTable vt = buildVirtualTable();
            DataStoreInfo dsInfo = getCatalog().getStore(storeId, DataStoreInfo.class);
            JDBCDataStore ds = (JDBCDataStore) dsInfo.getDataStore(null);
            ds.createVirtualTable(vt);

            CatalogBuilder builder = new CatalogBuilder(getCatalog());
            builder.setStore(dsInfo);
            FeatureTypeInfo fti = builder.buildFeatureType(ds.getFeatureSource(vt.getName()));
            fti.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
            LayerInfo layerInfo = builder.buildLayer(fti);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(
                    new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                            .getString());
        }
    }

    protected void onCancel() {
        doReturn(LayerPage.class);
    }
}
