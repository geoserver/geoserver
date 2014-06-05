package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.PageParameters;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.impl.WFSContentDataStore;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

public class CascadedWFSStoredQueryNewPage extends CascadedWFSStoredQueryAbstractPage {

    static final Logger LOGGER = Logging.getLogger(CascadedWFSStoredQueryNewPage.class);
    
    public CascadedWFSStoredQueryNewPage(PageParameters params) throws IOException {
        super(params);
    }
    
    @Override
    protected void onSave(String storedQueryId, StoredQueryConfiguration config) {
        try {
            DataStoreInfo dsInfo = getCatalog().getStore(storeId, DataStoreInfo.class);
            WFSContentDataStore directDs = getContentDataStore();
            DataAccess da = dsInfo.getDataStore(null);
            
            String localName = storedQueryId;
            // Transform type name if the DS retypes 
            if (da instanceof RetypingDataStore) {
                localName = ((RetypingDataStore)da).transformFeatureTypeName(localName);
            }
            
            Name typeName = directDs.createStoredQueryName(localName);
            
            CatalogBuilder builder = new CatalogBuilder(getCatalog());
            builder.setStore(dsInfo);
            FeatureTypeInfo fti = builder.buildFeatureType(da.getFeatureSource(typeName));

            fti.getMetadata().put(WFSContentDataStore.STORED_QUERY_CONFIGURATION_HINT, config);
            LayerInfo layerInfo = builder.buildLayer(fti);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));

        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(new ParamResourceModel("creationFailure", this, e.getMessage())
                    .getString());
        }
    }
    
    @Override
    protected void onCancel() {
        doReturn(LayerPage.class);     
    }
}
