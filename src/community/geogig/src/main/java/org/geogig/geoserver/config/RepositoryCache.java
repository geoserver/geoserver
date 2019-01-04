/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryResolver;

class RepositoryCache {

    private static final Logger LOGGER = Logging.getLogger(RepositoryCache.class);

    private final LoadingCache<String, Repository> repoCache;

    public RepositoryCache(final RepositoryManager repoManager) {

        RemovalListener<String, Repository> disposingListener =
                new RemovalListener<String, Repository>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, Repository> notification) {
                        String repoId = notification.getKey();
                        Repository repository = notification.getValue();
                        if (repository != null) {
                            try {
                                URI location = repository.getLocation();
                                LOGGER.fine(
                                        format(
                                                "Closing cached GeoGig repository instance %s",
                                                location != null ? location : repoId));
                                repository.close();
                                LOGGER.finer(
                                        format(
                                                "Closed cached GeoGig repository instance %s",
                                                location != null ? location : repoId));
                            } catch (RuntimeException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        format(
                                                "Error disposing GeoGig repository instance for id %s",
                                                repoId),
                                        e);
                            }
                        }
                    }
                };

        final CacheLoader<String, Repository> loader =
                new CacheLoader<String, Repository>() {
                    private final RepositoryManager manager = repoManager;

                    @Override
                    public Repository load(final String repoId) throws Exception {
                        try {
                            RepositoryInfo repoInfo = manager.get(repoId);
                            URI repoLocation = repoInfo.getLocation();
                            // RepositoryResolver.load returns an open repository or fails
                            Repository repo = RepositoryResolver.load(repoLocation);
                            checkState(repo.isOpen());

                            return repo;
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    format(
                                            "Error loading GeoGig repository instance for id %s",
                                            repoId),
                                    e);
                            throw e;
                        }
                    }
                };

        repoCache =
                CacheBuilder.newBuilder() //
                        .softValues() //
                        .expireAfterAccess(5, TimeUnit.MINUTES) //
                        .removalListener(disposingListener) //
                        .build(loader);
    }

    /**
     * @implNote: the returned repository's close() method does nothing. Closing the repository
     *     happens when it's evicted from the cache or is removed. This avoids several errors as
     *     GeoSever can aggressively create and dispose DataStores, whose dispose() method would
     *     otherwise close the repository and produce unexpected exceptions for any other code using
     *     it.
     */
    public Repository get(final String repositoryId) throws IOException {
        try {
            return repoCache.get(repositoryId);
        } catch (Throwable e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            Throwable cause = e.getCause();
            throw new IOException(
                    "Error obtaining cached geogig instance for repo "
                            + repositoryId
                            + ": "
                            + cause.getMessage(),
                    cause);
        }
    }

    public void invalidate(final String repoId) {
        repoCache.invalidate(repoId);
    }

    public void invalidateAll() {
        repoCache.invalidateAll();
        repoCache.cleanUp();
    }
}
