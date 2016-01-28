/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.RESOLVER_CLASS_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class GeoGigInitializer implements GeoServerInitializer {

    private static final Logger LOGGER = Logging.getLogger(GeoGigInitializer.class);

    private ConfigStore store;

    public static final String REPO_RESOLVER_CLASSNAME = GeoServerStoreRepositoryResolver.class
            .getName();

    public GeoGigInitializer(ConfigStore store) {
        this.store = store;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        // create RepositoryInfos for each datastore that doesn't have it to preserve backwards
        // compatibility
        Catalog catalog = geoServer.getCatalog();
        Map<String, RepositoryInfo> allByLocation = getAllByLocation();

        Multimap<String, DataStoreInfo> byRepo = storesByRepository(catalog);

        for (String repoDirectory : byRepo.keySet()) {

            if (!allByLocation.containsKey(repoDirectory)) {

                final RepositoryInfo info = create(repoDirectory);

                for (DataStoreInfo store : byRepo.get(repoDirectory)) {
                    LOGGER.info(format(
                            "Upgrading config for GeoGig store %s to refer to GeoServer's RepositoryInfo %s",
                            store.getName(), info.getId()));
                    Map<String, Serializable> params = store.getConnectionParameters();
                    params.put(REPOSITORY.key, info.getId());
                    params.put(RESOLVER_CLASS_NAME.key, REPO_RESOLVER_CLASSNAME);
                    catalog.save(store);
                }
            }
        }

        catalog.addListener(new DeprecatedDataStoreConfigFixer());
    }

    private Map<String, RepositoryInfo> getAllByLocation() {
        Map<String, RepositoryInfo> byLocation = new HashMap<>();
        for (RepositoryInfo info : store.getRepositories()) {
            byLocation.put(info.getLocation(), info);
        }
        return byLocation;
    }

    private RepositoryInfo create(String repoDirectory) {
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(repoDirectory);
        store.save(info);
        return info;
    }

    /**
     * Finds any geogig {@link DataStoreInfo} who's configuration doesn't have the
     * {@link GeoGigDataStoreFactory#RESOLVER_CLASS_NAME} set to GeoServerStoreRepositoryResolver's
     * class name, in order to upgrade its configuration for the geoserver resolver to take place
     */
    private Multimap<String, DataStoreInfo> storesByRepository(Catalog catalog) {
        List<DataStoreInfo> stores;
        stores = RepositoryManager.findGeogigStoresWithOldConfiguration(catalog);

        ListMultimap<String, DataStoreInfo> multimap = ArrayListMultimap.create();
        for (DataStoreInfo ds : stores) {
            Serializable configuredResolver = ds.getConnectionParameters().get(
                    RESOLVER_CLASS_NAME.key);
            if (!REPO_RESOLVER_CLASSNAME.equals(configuredResolver)) {
                multimap.put(repo(ds), ds);
            }
        }
        return multimap;
    }

    private String repo(DataStoreInfo ds) {
        Serializable value = ds.getConnectionParameters()
                .get(GeoGigDataStoreFactory.REPOSITORY.key);
        checkArgument(value != null, "%s not present in %s", GeoGigDataStoreFactory.REPOSITORY.key,
                ds);
        return String.valueOf(value);
    }

}
