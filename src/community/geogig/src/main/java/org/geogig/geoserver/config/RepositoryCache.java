/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.geogig.api.GeoGIG;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

class RepositoryCache {

    private static final Logger LOGGER = Logging.getLogger(RepositoryCache.class);

    private LoadingCache<String, GeoGIG> repoCache;

    public RepositoryCache(final RepositoryManager repoManager) {

        RemovalListener<String, GeoGIG> listener = new RemovalListener<String, GeoGIG>() {
            @Override
            public void onRemoval(RemovalNotification<String, GeoGIG> notification) {
                String repoId = notification.getKey();
                GeoGIG geogig = notification.getValue();
                if (geogig != null) {
                    try {
                        URI location = geogig.getRepository().getLocation();
                        LOGGER.fine(format("Closing cached GeoGig repository instance %s", location));
                        geogig.close();
                        LOGGER.finer(format("Closed cached GeoGig repository instance %s", location));
                    } catch (RuntimeException e) {
                        LOGGER.log(
                                Level.WARNING,
                                format("Error disposing GeoGig repository instance for id %s",
                                        repoId), e);
                    }
                }
            }
        };

        final CacheLoader<String, GeoGIG> loader = new CacheLoader<String, GeoGIG>() {
            private RepositoryManager manager = repoManager;

            @Override
            public GeoGIG load(final String repoId) throws Exception {
                try {
                    RepositoryInfo repoInfo = manager.get(repoId);
                    String repoLocation = repoInfo.getLocation();
                    File repoDir = new File(repoLocation);
                    GeoGIG geogig = new GeoGIG(repoDir);
                    geogig.getRepository();
                    return geogig;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            format("Error loading GeoGig repository instance for id %s", repoId), e);
                    throw e;
                }
            }
        };

        repoCache = CacheBuilder.newBuilder()//
                .softValues()//
                .expireAfterAccess(5, TimeUnit.MINUTES)//
                .removalListener(listener)//
                .build(loader);
    }

    public GeoGIG get(String repositoryId) throws IOException {
        try {
            return repoCache.get(repositoryId);
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            throw new IOException(
                    "Error obtaining cached geogig instance for repo " + repositoryId, e.getCause());
        }
    }

    public void invalidate(final String repoId) {
        repoCache.invalidate(repoId);
    }

    public void invalidateAll() {
        repoCache.invalidateAll();
    }
}
