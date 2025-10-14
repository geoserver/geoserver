/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.Repository;
import org.geotools.api.feature.type.Name;
import org.geotools.util.logging.Logging;

/**
 * Implementation of GeoTools Repository interface wrapped around the GeoServer catalog.
 *
 * @author Christian Mueller
 * @author Justin Deoliveira
 */
public class CatalogRepository implements Repository, Serializable {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog");

    /** the geoserver catalog */
    private Catalog catalog;

    public CatalogRepository() {}

    public CatalogRepository(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public DataStore dataStore(Name name) {
        DataAccess da = access(name);
        if (da instanceof DataStore store) {
            return store;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(name + " is not a data store.");
        }
        return null;
    }

    @Override
    public DataAccess<?, ?> access(Name name) {
        String workspace = name.getNamespaceURI();
        String localName = name.getLocalPart();

        DataStoreInfo info = getCatalog().getDataStoreByName(workspace, localName);
        if (info == null) {
            info = getCatalog().getDataStoreByName(localName);
            if (info == null) {
                return null;
            }
        }
        try {
            return info.getDataStore(null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<DataStore> getDataStores() {
        List<DataStore> dataStores = new ArrayList<>();
        for (DataStoreInfo ds : getCatalog().getDataStores()) {
            if (!ds.isEnabled()) {
                continue;
            }

            try {
                DataAccess da = ds.getDataStore(null);
                if (da instanceof DataStore store) {
                    dataStores.add(store);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to get datastore '" + ds.getName() + "'", e);
            }
        }
        return dataStores;
    }

    /** Accessor for the GeoServer catalog. */
    public Catalog getCatalog() {
        if (catalog != null) {
            return catalog;
        }

        catalog = GeoServerExtensions.bean(Catalog.class);
        if (catalog == null) {
            LOGGER.severe("Could not locate geoserver catalog");
        }
        return catalog;
    }
}
