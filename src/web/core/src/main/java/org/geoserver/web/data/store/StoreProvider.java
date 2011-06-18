/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Data providers for the {@link StorePanel}
 */
@SuppressWarnings("serial")
public class StoreProvider extends GeoServerDataProvider<StoreInfo> {
    
    static final Property<StoreInfo> TYPE = new AbstractProperty<StoreInfo>("type") {

        public IModel getModel(final IModel itemModel) {
            return new Model(itemModel) {
                
                @Override
                public Serializable getObject() {
                    StoreInfo si = (StoreInfo) itemModel.getObject();
                    return (String) getPropertyValue(si);
                }
            };
        }

        public Object getPropertyValue(StoreInfo item) {
            if (item instanceof DataStoreInfo)
                return "vector";
            else
                return "raster";
        }
    };

    static final Property<StoreInfo> WORKSPACE = new BeanProperty<StoreInfo>(
            "workspace", "workspace.name");

    static final Property<StoreInfo> NAME = new BeanProperty<StoreInfo>("name",
            "name");

    static final Property<StoreInfo> ENABLED = new BeanProperty<StoreInfo>(
            "enabled", "enabled");
    
    static final List<Property<StoreInfo>> PROPERTIES = Arrays.asList(TYPE,
            WORKSPACE, NAME, ENABLED);

    WorkspaceInfo workspace;
    
    public StoreProvider() {
        this(null);
    }
    
    public StoreProvider(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }
    
    @Override
    protected List<StoreInfo> getItems() {
        return workspace == null ? getCatalog().getStores(StoreInfo.class) 
            : getCatalog().getStoresByWorkspace( workspace, StoreInfo.class );
    }

    @Override
    protected List<Property<StoreInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected Comparator<StoreInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }
    

    public IModel newModel(Object object) {
        return new StoreInfoDetachableModel((StoreInfo) object);
    }


    /**
     * A StoreInfo detachable model that holds the store id to retrieve it on demand from the
     * catalog
     */
    static class StoreInfoDetachableModel extends LoadableDetachableModel {

        private static final long serialVersionUID = -6829878983583733186L;

        String id;

        public StoreInfoDetachableModel(StoreInfo store) {
            super(store);
            this.id = store.getId();
        }

        @Override
        protected Object load() {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            StoreInfo storeInfo = catalog.getStore(id, StoreInfo.class);
            return storeInfo;
        }
    }
}
