/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.annotation.Nullable;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.locationtech.geogig.cli.CLIContextBuilder;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.porcelain.BranchListOp;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.remote.IRemoteRepo;
import org.locationtech.geogig.remote.RemoteUtils;
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

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class RepositoryManager implements GeoServerInitializer {
    static {
        if (GlobalContextBuilder.builder() == null
                || GlobalContextBuilder.builder().getClass().equals(ContextBuilder.class)) {
            GlobalContextBuilder.builder(new CLIContextBuilder());
        }
    }

    private static class StaticSupplier implements Supplier<RepositoryManager>, Serializable {
        private static final long serialVersionUID = 3706728433275296134L;

        @Override
        public RepositoryManager get() {
            return RepositoryManager.get();
        }
    }

    private final ConfigStore store;

    private final RepositoryCache repoCache;

    private static RepositoryManager INSTANCE;

    private Catalog catalog;

    public static synchronized RepositoryManager get() {
        if (INSTANCE == null) {
            INSTANCE = GeoServerExtensions.bean(RepositoryManager.class);
            Preconditions.checkState(INSTANCE != null);
        }
        return INSTANCE;
    }

    public static void close() {
        if (INSTANCE != null) {
            INSTANCE.repoCache.invalidateAll();
            INSTANCE = null;
        }
    }

    public static Supplier<RepositoryManager> supplier() {
        return new StaticSupplier();
    }

    public RepositoryManager(ConfigStore store) {
        checkNotNull(store);
        this.store = store;
        this.repoCache = new RepositoryCache(this);
    }

    @Override
    public void initialize(GeoServer geoServer) {
        // set the catalog\
        setCatalog(geoServer.getCatalog());
    }

    public List<RepositoryInfo> getAll() {
        return store.getRepositories();
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
            Resource root = store.getConfigRoot();
            File parent = root.parent().dir().getAbsoluteFile();
            File f = new File(parent, UUID.randomUUID().toString());
            repoURI = f.toURI().normalize();
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

    public RepositoryInfo get(final String repoId) throws IOException {
        try {
            return store.get(repoId);
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException("No repository with ID " + repoId + " exists");
        }
    }

    public RepositoryInfo getByRepoName(final String name) {
        List<RepositoryInfo> all = getAll();
        for (RepositoryInfo info : all) {
            if (info.getRepoName().equals(name)) {
                return info;
            }
        }
        // didn't find it
        throw new NoSuchElementException("No repository with ID " + name + " exists");
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

    static List<DataStoreInfo> findGeogigStoresWithOldConfiguration(Catalog catalog) {
        // NOTE: Using a pre Filter and a post Predicate instead of a single AND Filter because
        // JDBCConfig doesn't know how to translate a connectionParameters.resolver Filter into an
        // SQL expression
        org.opengis.filter.Filter preFilter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);
        List<DataStoreInfo> stores = findGeoGigStores(catalog, preFilter);

        Predicate<DataStoreInfo> postFilter = new Predicate<DataStoreInfo>() {
            @Override
            public boolean apply(DataStoreInfo st) {
                final Map<String, Serializable> conParams = st.getConnectionParameters();
                if (conParams.containsKey(REPOSITORY.key)) {
                    final File repoConfigFile = new File(conParams.get(REPOSITORY.key).toString());
                    if (repoConfigFile.isDirectory()) {
                        return true;
                    }
                }
                return false;
            }
        };

        return Lists.newArrayList(Iterables.filter(stores, postFilter));
    }

    private static List<DataStoreInfo> findGeoGigStores(Catalog catalog,
            org.opengis.filter.Filter filter) {
        List<DataStoreInfo> geogigStores;
        try (CloseableIterator<DataStoreInfo> dataStores = catalog.list(DataStoreInfo.class, filter)) {
            geogigStores = Lists.newArrayList(dataStores);
        }

        return geogigStores;
    }

    public List<DataStoreInfo> findDataStores(final String repoId) {
        // get the name
        String repoName = null;
        try {
            repoName = this.get(repoId).getRepoName();
        } catch (IOException ioe) {
            Throwables.propagate(ioe);
        }
        Filter filter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);

        String locationKey = "connectionParameters." + GeoGigDataStoreFactory.REPOSITORY.key;
        filter = and(filter, equal(locationKey, GeoServerGeoGigRepositoryResolver.getURI(repoName)));
        List<DataStoreInfo> dependent;
        try (CloseableIterator<DataStoreInfo> stores = this.catalog.list(DataStoreInfo.class,
                filter)) {
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
        try (CloseableIterator<FeatureTypeInfo> it = this.catalog.list(FeatureTypeInfo.class,
                filter)) {
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
                    getRepository(oldRepo.getId()).command(ConfigOp.class)
                            .setAction(ConfigOp.ConfigAction.CONFIG_SET).setName("repo.name")
                            .setScope(ConfigOp.ConfigScope.LOCAL).setValue(newName).call();
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
            try {
                RepositoryInfo currentInfo = get(info.getId());
                handleRepoRename(currentInfo, info);
            } catch (IOException ioe) {

            }

        }
        // so far we don't need to invalidate the GeoGIG instance from the cache here... re-evaluate
        // if any configuration option would require so in the future
        return store.save(info);
    }

    private void create(final RepositoryInfo repoInfo) {
        // File targetDirectory = new File(repoInfo.getLocation());
        // Preconditions.checkArgument(!isGeogigDirectory(targetDirectory));

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
            this.store.delete(repoId);
        } finally {
            this.repoCache.invalidate(repoId);
        }
    }

    RepositoryInfo findOrCreateByLocation(final URI repositoryURI) {
        List<RepositoryInfo> repos = getAll();
        for (RepositoryInfo info : repos) {
            if (Objects.equal(info.getLocation(), repositoryURI)) {
                return info;
            }
        }
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(repositoryURI);
        return save(info);
    }

    /**
     * Utility class to connect to a remote to see if its alive and we're able to connect.
     * 
     * @return the remote's head ref if succeeded
     * @throws Exception if can't connect for any reason; the exception message should be indicative
     *         of the problem
     */
    public static Ref pingRemote(final String location, @Nullable String user,
            @Nullable String password) throws Exception {

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
            remote = new Remote(name, fetchurl, pushurl, fetch, mapped, mappedBranch, user,
                    password);
        }

        return pingRemote(remote);
    }

    private static Ref pingRemote(Remote remote) throws Exception {

        Optional<IRemoteRepo> remoteRepo;
        try {
            Hints hints = Hints.readOnly();
            Repository localRepo = GlobalContextBuilder.builder().build(hints).repository();
            remoteRepo = RemoteUtils.newRemote(localRepo, remote, null);
            if (!remoteRepo.isPresent()) {
                throw new IllegalArgumentException("Repository not found or not reachable");
            } else {
                IRemoteRepo repo = remoteRepo.get();
                try {
                    repo.open();
                    Ref head = repo.headRef();
                    return head;
                } finally {
                    repo.close();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to connect: " + e.getMessage(), e);
        }
    }
}
