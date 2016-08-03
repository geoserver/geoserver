/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.geogig.model.Node;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Platform;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.StagingArea;
import org.locationtech.geogig.repository.WorkingTree;
import org.locationtech.geogig.storage.BlobStore;
import org.locationtech.geogig.storage.ConfigDatabase;
import org.locationtech.geogig.storage.ConflictsDatabase;
import org.locationtech.geogig.storage.GraphDatabase;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.storage.RefDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

class RepositoryCache {

    private static final Logger LOGGER = Logging.getLogger(RepositoryCache.class);

    private final LoadingCache<String, Repository> repoCache;

    public RepositoryCache(final RepositoryManager repoManager) {

        RemovalListener<String, Repository> disposingListener = new RemovalListener<String, Repository>() {
            @Override
            public void onRemoval(RemovalNotification<String, Repository> notification) {
                String repoId = notification.getKey();
                Repository repository = notification.getValue();
                if (repository != null) {
                    try {
                        URI location = repository.getLocation();
                        LOGGER.fine(format("Closing cached GeoGig repository instance %s",
                                location != null ? location : repoId));
                        repository.close();
                        LOGGER.finer(format("Closed cached GeoGig repository instance %s",
                                location != null ? location : repoId));
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.WARNING, format(
                                "Error disposing GeoGig repository instance for id %s", repoId), e);
                    }
                }
            }
        };

        final CacheLoader<String, Repository> loader = new CacheLoader<String, Repository>() {
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
                    LOGGER.log(Level.WARNING,
                            format("Error loading GeoGig repository instance for id %s", repoId),
                            e);
                    throw e;
                }
            }
        };

        repoCache = CacheBuilder.newBuilder()//
                .softValues()//
                .expireAfterAccess(5, TimeUnit.MINUTES)//
                .removalListener(disposingListener)//
                .build(loader);
    }

    /**
     * @implNote: the returned repository's close() method does nothing. Closing the repository
     *            happens when it's evicted from the cache or is removed. This avoids several errors
     *            as GeoSever can aggressively create and dispose DataStores, whose dispose() method
     *            would otherwise close the repository and produce unexpected exceptions for any
     *            other code using it.
     */
    public Repository get(final String repositoryId) throws IOException {
        try {
            Repository repo = repoCache.get(repositoryId);
            return new UnclosableRepository(repo);
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            throw new IOException("Error obtaining cached geogig instance for repo " + repositoryId,
                    e.getCause());
        }
    }

    public void invalidate(final String repoId) {
        repoCache.invalidate(repoId);
    }

    public void invalidateAll() {
        repoCache.invalidateAll();
    }

    private static class UnclosableRepository implements Repository {

        private final Repository subject;

        UnclosableRepository(Repository subject) {
            this.subject = subject;
        }

        @Override
        public void close() {
            // ignored
        }

        @Override
        public void addListener(RepositoryListener listener) {
            subject.addListener(listener);
        }

        @Override
        public void configure() throws RepositoryConnectionException {
            subject.configure();
        }

        @Override
        public boolean isOpen() {
            return subject.isOpen();
        }

        @Override
        public void open() throws RepositoryConnectionException {
            subject.open();
        }

        @Override
        public URI getLocation() {
            return subject.getLocation();
        }

        @Override
        public <T extends AbstractGeoGigOp<?>> T command(Class<T> commandClass) {
            return subject.command(commandClass);
        }

        @Override
        public boolean blobExists(ObjectId id) {
            return subject.blobExists(id);
        }

        @Override
        public Optional<Ref> getRef(String revStr) {
            return subject.getRef(revStr);
        }

        @Override
        public Optional<Ref> getHead() {
            return subject.getHead();
        }

        @Override
        public boolean commitExists(ObjectId id) {
            return subject.commitExists(id);
        }

        @Override
        public RevCommit getCommit(ObjectId commitId) {
            return subject.getCommit(commitId);
        }

        @Override
        public boolean treeExists(ObjectId id) {
            return subject.treeExists(id);
        }

        @Override
        public ObjectId getRootTreeId() {
            return subject.getRootTreeId();
        }

        @Override
        public RevFeature getFeature(ObjectId contentId) {
            return subject.getFeature(contentId);
        }

        @Override
        public RevTree getOrCreateHeadTree() {
            return subject.getOrCreateHeadTree();
        }

        @Override
        public RevTree getTree(ObjectId treeId) {
            return subject.getTree(treeId);
        }

        @Override
        public Optional<Node> getRootTreeChild(String path) {
            return subject.getRootTreeChild(path);
        }

        @Override
        public Optional<Node> getTreeChild(RevTree tree, String childPath) {
            return subject.getTreeChild(tree, childPath);
        }

        @Override
        public Optional<Integer> getDepth() {
            return subject.getDepth();
        }

        @Override
        public boolean isSparse() {
            return subject.isSparse();
        }

        @Override
        public Context context() {
            return subject.context();
        }

        @Override
        public WorkingTree workingTree() {
            return subject.workingTree();
        }

        @Override
        public StagingArea index() {
            return subject.index();
        }

        @Override
        public RefDatabase refDatabase() {
            return subject.refDatabase();
        }

        @Override
        public Platform platform() {
            return subject.platform();
        }

        @Override
        public ObjectDatabase objectDatabase() {
            return subject.objectDatabase();
        }

        @Override
        public ConflictsDatabase conflictsDatabase() {
            return subject.conflictsDatabase();
        }

        @Override
        public ConfigDatabase configDatabase() {
            return subject.configDatabase();
        }

        @Override
        public GraphDatabase graphDatabase() {
            return subject.graphDatabase();
        }

        @Override
        public BlobStore blobStore() {
            return subject.blobStore();
        }

    }
}
