/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.LockingManager;
/** 
 * 
 * @author Arne Kepp, The Open Planning Project
 */
@SuppressWarnings("serial")
public abstract class ServerAdminPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = 4712657652337914993L;

    public IModel getGeoServerModel(){
        return new LoadableDetachableModel(){
            public Object load() {
                return getGeoServerApplication().getGeoServer();
            }
        };
    }

    public IModel getGlobalInfoModel(){
        return new LoadableDetachableModel(){
            public Object load() {
                return getGeoServerApplication().getGeoServer().getGlobal();
            }
        };
    }

    public IModel getJAIModel(){
        return new LoadableDetachableModel(){
            public Object load() {
                return getGeoServerApplication()
                    .getGeoServer().getGlobal().getJAI();
            }
        };
    }
    
    public IModel getCoverageAccessModel(){
        return new LoadableDetachableModel(){
            public Object load() {
                return getGeoServerApplication()
                    .getGeoServer().getGlobal().getCoverageAccess();
            }
        };
    }

    public IModel getContactInfoModel(){
        return new LoadableDetachableModel(){
            public Object load() {
                return getGeoServerApplication()
                    .getGeoServer()
                    .getGlobal()
                    .getContact();
            }
        };
    }
    
    public IModel getLoggingInfoModel() {
        return new LoadableDetachableModel() {
            @Override
            protected Object load() {
                return getGeoServer().getLogging();
            }
        };
    }

    private synchronized int getLockCount(){
        int count = 0;

        for (Iterator i = getDataStores().iterator(); i.hasNext();) {
            DataStoreInfo meta = (DataStoreInfo) i.next();

            if (!meta.isEnabled()) {
                // Don't count locks from disabled datastores.
                continue;
            }

            try {
                DataAccess store = meta.getDataStore(null);
                if(store instanceof DataStore) {
                    LockingManager lockingManager = ((DataStore) store).getLockingManager();
                    if (lockingManager != null){
                        // we can't actually *count* locks right now?
                        // count += lockingManager.getLockSet().size();
                    }
                }
            } catch (IllegalStateException notAvailable) {
                continue; 
            } catch (Throwable huh) {
                continue;
            }
        }

        return count;
    }

    private synchronized int getConnectionCount() {
        int count = 0;

        for (Iterator i = getDataStores().iterator(); i.hasNext();) {
            DataStoreInfo meta = (DataStoreInfo) i.next();

            if (!meta.isEnabled()) {
                // Don't count connections from disabled datastores.
                continue; 
            }

            try {
                meta.getDataStore(null);
            } catch (Throwable notAvailable) {
                //TODO: Logging.
                continue; 
            }

            count += 1;
        }

        return count;
    }

    private List<DataStoreInfo> getDataStores(){
        return getGeoServerApplication().getGeoServer().getCatalog().getDataStores();
    }
}

