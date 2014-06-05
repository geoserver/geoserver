package org.geoserver.web.data.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.StoredQueryDescriptionType;
import net.opengis.wfs20.StoredQueryListItemType;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
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
    
    DropDownChoice storedQueriesDropDown;
    
    public CascadedWFSStoredQueryNewPage(PageParameters params) throws IOException {
        super(params);
    }
    
    @Override
    protected Component getStoredQueryNameComponent() {
        Fragment f = new Fragment("storedQueryName", "newPage", this);
        storedQueriesDropDown = storedQueriesDropDown();
        f.add(storedQueriesDropDown);
        
        return f;
    }

    @Override
    protected void onSave() {
        
        StoredQuery selection = (StoredQuery)storedQueriesDropDown.getDefaultModelObject();
        StoredQueryConfiguration config = createStoredQueryConfiguration(parameterProvider.getItems());
        
        String storedQueryId = selection.storedQueryId;
        
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
    

    private DropDownChoice storedQueriesDropDown() {
        final DropDownChoice dropdown = new DropDownChoice("storedQueriesDropDown", new Model(),
                new StoredQueryListModel(), new StoredQueryListRenderer());
        
        dropdown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                StoredQuery selection = (StoredQuery)dropdown.getDefaultModelObject();
                parameterProvider.refreshItems(selection.storedQueryId);
                target.addComponent(parameters);
            }
        });
        
        return dropdown;
    }
    
    private class StoredQueryListModel extends LoadableDetachableModel<List<StoredQuery>> {
        @Override
        protected List<StoredQuery> load() {
            List<StoredQuery> ret = new ArrayList<StoredQuery>();
            
            for (StoredQueryListItemType sqlit : listStoredQueries()) {
                StoredQuery item = new StoredQuery();
                item.setStoredQueryId(sqlit.getId());
                item.setTitle(createStoredQueryTitle(sqlit));
                
                ret.add(item);
            }
            return ret;
        }
    }
    
    private class StoredQueryListRenderer implements IChoiceRenderer<StoredQuery> {
        @Override
        public Object getDisplayValue(StoredQuery object) {
            return object.getTitle();
        }
        
        @Override
        public String getIdValue(StoredQuery object, int index) {
            return object.getStoredQueryId();
        }
    }

    
    public static class StoredQuery implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String storedQueryId;
        
        public void setStoredQueryId(String storedQueryId) {
            this.storedQueryId = storedQueryId;
        }
        
        public String getStoredQueryId() {
            return storedQueryId;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getTitle() {
            return title;
        }
    }
    
}
