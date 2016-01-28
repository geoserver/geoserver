/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.CREATE;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.RESOLVER_CLASS_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geotools.data.DataAccess;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * {@link CatalogListener} to make sure GeoGig datastores are saved with the correct set of
 * connection parameters.
 * <p>
 * For instance, of the repository directory path is given as the
 * {@link GeoGigDataStoreFactory#REPOSITORY} parameter, this listener will make sure there exists a
 * {@link RepositoryInfo} config for that repository, as managed by this geoserver plugin, and that
 * the {@code REPOSITORY} parameter is set to the {@link RepositoryInfo#getId() repository info id}
 * instead. Plus, it'll set the {@link GeoGigDataStoreFactory#RESOLVER_CLASS_NAME} connection
 * parameter to {@link GeoServerStoreRepositoryResolver
 * org.geogig.geoserver.config.GeoServerStoreRepositoryResolver}
 *
 * <p>
 * This listener is added to the catalog by {@link GeoGigInitializer}
 */
class DeprecatedDataStoreConfigFixer implements CatalogListener {

    private static final Logger LOGGER = Logging.getLogger(DeprecatedDataStoreConfigFixer.class);

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        checkNotNull(event);
        try {
            ensureNewConfig(event.getSource());
        } catch (RuntimeException rte) {
            rte.printStackTrace();
            LOGGER.log(Level.WARNING,
                    "Unexpected exception handing add event on " + event.getSource()
                            + ". Returning silently to let the catalog go on.", rte);
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        checkNotNull(event);
        try {
            ensureNewConfig(event.getSource());
        } catch (RuntimeException rte) {
            LOGGER.log(Level.WARNING,
                    "Unexpected exception handing post modify event on " + event.getSource()
                            + ". Returning silently to let the catalog go on.", rte);
        }
    }

    private void ensureNewConfig(CatalogInfo source) {
        if (!(source instanceof DataStoreInfo)) {
            return;
        }
        DataStoreInfo ds = (DataStoreInfo) source;
        Map<String, Serializable> params = ds.getConnectionParameters();
        if (null == params.get(REPOSITORY.key)) {
            return;
        }
        final String repositoryParam = String.valueOf(params.get(REPOSITORY.key));
        final String resolverParam = (String) params.get(RESOLVER_CLASS_NAME.key);
        RepositoryManager manager = RepositoryManager.get();
        if (GeoGigInitializer.REPO_RESOLVER_CLASSNAME.equals(resolverParam)) {
            // verify the repo info exists
            try {
                manager.get(repositoryParam);// is it a proper RepositoryInfo id?
            } catch (IOException e) {
                if (RepositoryManager.isGeogigDirectory(new File(repositoryParam))) {
                    // this is wrong, resolver is right but given a location path instead, fix it.
                    fixConfig(ds, repositoryParam);
                } else {
                    // we tried...
                    String msg = String
                            .format("GeoGig DataStore config has repository %s but it couldn't be resolverd to an actual repository",
                                    repositoryParam);
                    LOGGER.log(Level.WARNING, msg, e);
                }
            }
        } else {
            final File repoDirectory = new File(repositoryParam);
            if (!RepositoryManager.isGeogigDirectory(repoDirectory)) {
                try {
                    if (Boolean.TRUE.equals(CREATE.lookUp(params))) {
                        DataAccess<? extends FeatureType, ? extends Feature> dataStore;
                        dataStore = new GeoGigDataStoreFactory().createDataStore(params);
                        dataStore.dispose();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to create repository at "
                            + repoDirectory.getAbsolutePath(), e);
                }
            }
            if (RepositoryManager.isGeogigDirectory(repoDirectory)) {
                fixConfig(ds, repositoryParam);
            } else {
                String msg = String
                        .format("GeoGig DataStore config has repository %s but it couldn't be resolverd to an actual repository",
                                repositoryParam);
                LOGGER.log(Level.WARNING, msg);
            }
        }
    }

    private void fixConfig(final DataStoreInfo ds, final String repoDirectory) {
        RepositoryManager manager = RepositoryManager.get();
        Map<String, Serializable> params = ds.getConnectionParameters();
        RepositoryInfo info = manager.findOrCreateByLocation(repoDirectory);
        params.put(REPOSITORY.key, info.getId());
        params.put(RESOLVER_CLASS_NAME.key, GeoGigInitializer.REPO_RESOLVER_CLASSNAME);
        Catalog catalog = ds.getCatalog();
        catalog.save(ds);
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // nothing to do
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // nothing to do
    }

    @Override
    public void reloaded() {
        // nothing to do
    }

}
