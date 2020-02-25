/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.annotation.Nullable;
import org.geogig.geoserver.config.ConfigStore.RepositoryInfoChangedCallback;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.porcelain.BranchListOp;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.remotes.internal.IRemoteRepo;
import org.locationtech.geogig.remotes.internal.RemoteResolver;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.ContextBuilder;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.DisposableBean;

public class RepositoryManager implements GeoServerInitializer, DisposableBean {
    static {
        if (GlobalContextBuilder.builder() == null
                || GlobalContextBuilder.builder().getClass().equals(ContextBuilder.class)) {
            GlobalContextBuilder.builder(new GeoServerContextBuilder());
        }
    }

    private static final String REPO_ROOT = "geogig/repos";

    private ConfigStore configStore;

    private ResourceStore resourceStore;

    private RepositoryCache repoCache;

    private static RepositoryManager INSTANCE;

    private Catalog catalog;

    private RepositoryInfoChangedCallback REPO_CHANGED_CALLBACK;

    public static synchronized RepositoryManager get() {
        if (INSTANCE == null) {
            INSTANCE = GeoServerExtensions.bean(RepositoryManager.class);
            Preconditions.checkState(INSTANCE != null);
        }
        return INSTANCE;
    }

    public static synchronized void close() {
        if (INSTANCE != null) {
            INSTANCE.dispose();
            INSTANCE = null;
        }
    }

    public RepositoryManager(ConfigStore configStore, ResourceStore resourceStore) {
        init(configStore, resourceStore);
    }

    @VisibleForTesting
    RepositoryManager() {}

    @VisibleForTesting
    void init(ConfigStore configStore, ResourceStore resourceStore) {
        checkNotNull(configStore);
        checkNotNull(resourceStore);
        this.configStore = configStore;
        this.resourceStore = resourceStore;
        this.repoCache = new RepositoryCache(this);
        REPO_CHANGED_CALLBACK = (repoId) -> invalidate(repoId);
        this.configStore.addRepositoryInfoChangedCallback(REPO_CHANGED_CALLBACK);
    }

    @Override
    public void initialize(GeoServer geoServer) {
        // set the catalog\
        setCatalog(geoServer.getCatalog());
    }

    @Override
    public void destroy() throws Exception {
        dispose();
    }

    public void dispose() {
        configStore.removeRepositoryInfoChangedCallback(REPO_CHANGED_CALLBACK);
        repoCache.invalidateAll();
    }

    public List<RepositoryInfo> getAll() {
        return configStore.getRepositories();
    }

    public void invalidate(final String repoId) {
        this.repoCache.invalidate(repoId);
    }

    @Nullable
    public Repository createRepo(final Hints hints) {
        // get the Config store location
        // only generate a location if no URI is set in the hints
        URI repoURI;
        if (hints.get(Hints.REPOSITORY_URL).isPresent()) {
            repoURI = URI.create(hints.get(Hints.REPOSITORY_URL).get().toString());
        } else {
            // no location set yet, generate one
            // NOTE: If the resource store does not support a file system, the repository will be
            // created
            // in a temporary directory. If this is the case, remove any repository resolvers that
            // can
            // resolve a 'file' URI to prevent the creation of such repos.
            Resource root = resourceStore.get(REPO_ROOT);
            File repoDir = root.get(UUID.randomUUID().toString()).dir();
            if (!repoDir.exists()) {
                repoDir.mkdirs();
            }
            repoURI = repoDir.toURI().normalize();
            hints.set(Hints.REPOSITORY_URL, repoURI);
        }

        Context context = GlobalContextBuilder.builder().build(hints);
        RepositoryResolver repositoryResolver = RepositoryResolver.lookup(repoURI);
        final boolean exists = repositoryResolver.repoExists(repoURI);
        Repository repository = context.repository();
        if (exists) {
            try {
                repository.open();
            } catch (RepositoryConnectionException e) {
                throw Throwables.propagate(e);
            }
        }
        return repository;
    }

    public RepositoryInfo get(final String repoId) throws NoSuchElementException {
        return configStore.get(repoId);
    }

    /**
     * Retrieves a RepositoryInfo with a specified name.
     *
     * @param name The name of the repository desired.
     * @return a RepositoryInfo object, if found. If not found, returns null.
     */
    public @Nullable RepositoryInfo getByRepoName(final String name) {
        RepositoryInfo info = configStore.getByName(name);
        return info;
    }

    public @Nullable RepositoryInfo getByRepoLocation(final URI repoURI) {
        RepositoryInfo info = configStore.getByLocation(repoURI);
        return info;
    }

    public boolean repoExistsByName(final String name) {
        return configStore.repoExistsByName(name);
    }

    public boolean repoExistsByLocation(URI location) {
        return configStore.repoExistsByLocation(location);
    }

    public List<DataStoreInfo> findGeogigStores() {
        return findGeogigStores(this.catalog);
    }

    public Catalog getCatalog() {
        return this.catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    static List<DataStoreInfo> findGeogigStores(Catalog catalog) {
        org.opengis.filter.Filter filter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);
        return findGeoGigStores(catalog, filter);
    }

    private static List<DataStoreInfo> findGeoGigStores(
            Catalog catalog, org.opengis.filter.Filter filter) {
        List<DataStoreInfo> geogigStores;
        try (CloseableIterator<DataStoreInfo> dataStores =
                catalog.list(DataStoreInfo.class, filter)) {
            geogigStores = Lists.newArrayList(dataStores);
        }

        return geogigStores;
    }

    public List<DataStoreInfo> findDataStores(final String repoId) {
        // get the name
        String repoName = null;
        try {
            repoName = this.get(repoId).getRepoName();
        } catch (NoSuchElementException ioe) {
            Throwables.propagate(ioe);
        }
        Filter filter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);

        String locationKey = "connectionParameters." + GeoGigDataStoreFactory.REPOSITORY.key;
        filter =
                and(filter, equal(locationKey, GeoServerGeoGigRepositoryResolver.getURI(repoName)));
        List<DataStoreInfo> dependent;
        try (CloseableIterator<DataStoreInfo> stores =
                this.catalog.list(DataStoreInfo.class, filter)) {
            dependent = Lists.newArrayList(stores);
        }
        return dependent;
    }

    public List<? extends CatalogInfo> findDependentCatalogObjects(final String repoId) {
        List<DataStoreInfo> stores = findDataStores(repoId);
        List<CatalogInfo> dependent = new ArrayList<>(stores);
        for (DataStoreInfo dataStore : stores) {
            List<FeatureTypeInfo> ftypes = this.catalog.getFeatureTypesByDataStore(dataStore);
            dependent.addAll(ftypes);
            for (FeatureTypeInfo ftype : ftypes) {
                dependent.addAll(this.catalog.getLayers(ftype));
            }
        }

        return dependent;
    }

    public List<LayerInfo> findLayers(DataStoreInfo store) {
        Filter filter = equal("resource.store.id", store.getId());
        try (CloseableIterator<LayerInfo> it = this.catalog.list(LayerInfo.class, filter)) {
            return Lists.newArrayList(it);
        }
    }

    public List<FeatureTypeInfo> findFeatureTypes(DataStoreInfo store) {
        Filter filter = equal("store.id", store.getId());
        try (CloseableIterator<FeatureTypeInfo> it =
                this.catalog.list(FeatureTypeInfo.class, filter)) {
            return Lists.newArrayList(it);
        }
    }

    public static boolean isGeogigDirectory(final File file) {
        if (file == null) {
            return false;
        }
        final File geogigDir = new File(file, ".geogig");
        final boolean isGeogigDirectory = geogigDir.exists() && geogigDir.isDirectory();
        return isGeogigDirectory;
    }

    private void handleRepoRename(RepositoryInfo oldRepo, RepositoryInfo newRepo) {
        if (Objects.equal(oldRepo.getId(), newRepo.getId())) {
            // repos have the same ID, check the names
            final String oldName = oldRepo.getRepoName();
            final String newName = newRepo.getRepoName();
            if (!Objects.equal(oldName, newName)) {
                // name has been changed, update the repo
                try {
                    getRepository(oldRepo.getId())
                            .command(ConfigOp.class)
                            .setAction(ConfigOp.ConfigAction.CONFIG_SET)
                            .setName("repo.name")
                            .setScope(ConfigOp.ConfigScope.LOCAL)
                            .setValue(newName)
                            .call();
                } catch (IOException ioe) {
                    // log?
                }
            }
        }
    }

    public RepositoryInfo save(RepositoryInfo info) {
        Preconditions.checkNotNull(info.getLocation());
        if (info.getId() == null) {
            create(info);
        } else {
            // see if the name has changed. If so, update the repo config
            RepositoryInfo currentInfo = get(info.getId());
            handleRepoRename(currentInfo, info);
        }
        // so far we don't need to invalidate the GeoGIG instance from the cache here... re-evaluate
        // if any configuration option would require so in the future
        return configStore.save(info);
    }

    private void create(final RepositoryInfo repoInfo) {
        URI repoURI = repoInfo.getLocation();
        RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
        if (!resolver.repoExists(repoURI)) {
            Hints hints = new Hints();
            hints.set(Hints.REPOSITORY_URL, repoURI);
            hints.set(Hints.REPOSITORY_NAME, repoInfo.getRepoName());
            Context context = GlobalContextBuilder.builder().build(hints);
            GeoGIG geogig = new GeoGIG(context);
            try {
                Repository repository = geogig.command(InitOp.class).call();
                Preconditions.checkState(repository != null);
            } finally {
                geogig.close();
            }
        }
    }

    public List<Ref> listBranches(final String repositoryId) throws IOException {
        Repository geogig = getRepository(repositoryId);
        List<Ref> refs = geogig.command(BranchListOp.class).call();
        return refs;
    }

    public Repository getRepository(String repositoryId) throws IOException {
        Repository repository = repoCache.get(repositoryId);
        return repository;
    }

    public void delete(final String repoId) {
        List<DataStoreInfo> repoStores = findDataStores(repoId);
        CascadeDeleteVisitor deleteVisitor = new CascadeDeleteVisitor(this.catalog);
        for (DataStoreInfo storeInfo : repoStores) {
            storeInfo.accept(deleteVisitor);
        }
        try {
            this.configStore.delete(repoId);
        } finally {
            this.repoCache.invalidate(repoId);
        }
    }

    RepositoryInfo findOrCreateByLocation(final URI repositoryURI) {
        RepositoryInfo info = configStore.getByLocation(repositoryURI);
        if (info != null) {
            return info;
        }
        info = new RepositoryInfo();
        info.setLocation(repositoryURI);
        return save(info);
    }

    /**
     * Utility class to connect to a remote to see if its alive and we're able to connect.
     *
     * @return the remote's head ref if succeeded
     * @throws Exception if can't connect for any reason; the exception message should be indicative
     *     of the problem
     */
    public static Ref pingRemote(
            final String location, @Nullable String user, @Nullable String password)
            throws Exception {

        if (Strings.isNullOrEmpty(location)) {
            throw new IllegalArgumentException("Please indicate the remote repository URL");
        }
        Remote remote;
        {
            String fetchurl = location;
            String pushurl = location;
            String name = "tempremote";
            String fetch = "+" + Ref.HEADS_PREFIX + "*:" + Ref.REMOTES_PREFIX + name + "/*";
            boolean mapped = false;
            String mappedBranch = null;
            remote =
                    new Remote(
                            name, fetchurl, pushurl, fetch, mapped, mappedBranch, user, password);
        }

        return pingRemote(remote);
    }

    private static Ref pingRemote(Remote remote) throws Exception {

        Optional<IRemoteRepo> remoteRepo;
        try {
            Hints hints = Hints.readOnly();
            Repository localRepo = GlobalContextBuilder.builder().build(hints).repository();
            remoteRepo = RemoteResolver.newRemote(remote, null);
            if (!remoteRepo.isPresent()) {
                throw new IllegalArgumentException("Repository not found or not reachable");
            } else {
                IRemoteRepo repo = remoteRepo.get();
                try {
                    repo.open();
                    Optional<Ref> head = repo.headRef();
                    return head.orNull();
                } finally {
                    repo.close();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to connect: " + e.getMessage(), e);
        }
    }

    public boolean isGeogigLayer(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        if (resource == null) {
            return false;
        }
        StoreInfo store = resource.getStore();
        if (store == null) {
            return false;
        }
        return isGeogigStore(store);
    }

    public boolean isGeogigStore(CatalogInfo store) {
        if (!(store instanceof DataStoreInfo)) {
            return false;
        }
        final String storeType = ((DataStoreInfo) store).getType();
        boolean isGeogigStore = GeoGigDataStoreFactory.DISPLAY_NAME.equals(storeType);
        return isGeogigStore;
    }

    public Repository findRepository(LayerInfo geogigLayer) {
        Preconditions.checkArgument(isGeogigLayer(geogigLayer));

        Map<String, Serializable> params =
                geogigLayer.getResource().getStore().getConnectionParameters();
        String repoUriStr = String.valueOf(params.get(GeoGigDataStoreFactory.REPOSITORY.key));
        URI repoURI = URI.create(repoUriStr);
        RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
        String repoName = resolver.getName(repoURI);
        RepositoryInfo repoInfo = getByRepoName(repoName);
        String repoId = repoInfo.getId();
        try {
            Repository repository = getRepository(repoId);
            Preconditions.checkState(repository != null);
            return repository;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
