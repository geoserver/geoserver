/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.LockingManager;

/** @author Arne Kepp, The Open Planning Project */
@SuppressWarnings("serial")
public abstract class ServerAdminPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = 4712657652337914993L;

    public IModel<GeoServer> getGeoServerModel() {
        return new LoadableDetachableModel<GeoServer>() {
            public GeoServer load() {
                return getGeoServerApplication().getGeoServer();
            }
        };
    }

    public IModel<GeoServerInfo> getGlobalInfoModel() {
        return new LoadableDetachableModel<GeoServerInfo>() {
            public GeoServerInfo load() {
                return getGeoServerApplication().getGeoServer().getGlobal();
            }
        };
    }

    public IModel<JAIInfo> getJAIModel() {
        // Notes setup on top of an explanation provided by Gabriel Roldan for
        // his patch which fixes the modificationProxy unable to detect changes
        // --------------------------------------------------------------------
        // with this change, we will edit a clone of the original JAIInfo.
        // By this way, the modification proxy will count it as a change.
        // The previous code wasn't working as expected.
        // the reason is that the model used to edit JAIInfo is a
        // LoadableDetachableModel, so when the edit page does gobal.setJAI, it
        // is actually setting the same object reference, and hence the
        // modificationproxy does not count it as a change.

        JAIInfo currJaiInfo = getGeoServerApplication().getGeoServer().getGlobal().getJAI().clone();
        return new Model<JAIInfo>(currJaiInfo);
    }

    public IModel<CoverageAccessInfo> getCoverageAccessModel() {
        // Notes setup on top of an explanation provided by Gabriel Roldan for
        // his patch which fixes the modificationProxy unable to detect changes
        // --------------------------------------------------------------------
        // with this change, we will edit a clone of the original Info.
        // By this way, the modification proxy will count it as a change.
        // The previous code wasn't working as expected.
        // the reason is that the model used to edit the page is a
        // LoadableDetachableModel, so when the edit page does gobal.setJAI, it
        // is actually setting the same object reference, and hence the
        // modificationProxy does not count it as a change.

        CoverageAccessInfo currCoverageAccessInfo =
                getGeoServerApplication().getGeoServer().getGlobal().getCoverageAccess().clone();
        return new Model<CoverageAccessInfo>(currCoverageAccessInfo);
    }

    @Deprecated
    public IModel<ContactInfo> getContactInfoModel() {
        return new LoadableDetachableModel<ContactInfo>() {
            public ContactInfo load() {
                return getGeoServerApplication().getGeoServer().getGlobal().getContact();
            }
        };
    }

    public IModel<LoggingInfo> getLoggingInfoModel() {
        return new LoadableDetachableModel<LoggingInfo>() {
            @Override
            protected LoggingInfo load() {
                return getGeoServer().getLogging();
            }
        };
    }

    private synchronized int getLockCount() {
        int count = 0;

        for (DataStoreInfo meta : getDataStores()) {
            if (!meta.isEnabled()) {
                // Don't count locks from disabled datastores.
                continue;
            }

            try {
                DataAccess<?, ?> store = meta.getDataStore(null);
                if (store instanceof DataStore) {
                    LockingManager lockingManager = ((DataStore) store).getLockingManager();
                    if (lockingManager != null) {
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

        for (DataStoreInfo meta : getDataStores()) {
            if (!meta.isEnabled()) {
                // Don't count connections from disabled datastores.
                continue;
            }

            try {
                meta.getDataStore(null);
            } catch (Throwable notAvailable) {
                // TODO: Logging.
                continue;
            }

            count += 1;
        }

        return count;
    }

    private List<DataStoreInfo> getDataStores() {
        return getGeoServerApplication().getGeoServer().getCatalog().getDataStores();
    }
}
