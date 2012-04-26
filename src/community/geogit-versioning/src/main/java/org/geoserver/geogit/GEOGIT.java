/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geogit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.task.LongTaskMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import com.sleepycat.je.Environment;

/**
 * Service facade to GeoServer
 * 
 * @author Gabriel Roldan
 * 
 */
public class GEOGIT implements DisposableBean {

    private static final Logger LOGGER = Logging.getLogger(GEOGIT.class);

    public static final String VERSIONING_DATA_ROOT = "versioning_data";

    private static final String GEOGIT_REPO = "geogit_repo";

    private static final String GEOGIT_INDEX = "geogit_index";

    private final Catalog catalog;

    private final GeoGIT geoGit;

    private final File baseRepoDir;

    private static GEOGIT singleton;

    public GEOGIT(final Catalog catalog, final GeoServerDataDirectory dataDir) throws IOException {
        this.catalog = catalog;
        this.baseRepoDir = dataDir.findOrCreateDataDir(VERSIONING_DATA_ROOT);

        final File geogitRepo = dataDir.findOrCreateDataDir(VERSIONING_DATA_ROOT, GEOGIT_REPO);
        final File indexRepo = dataDir.findOrCreateDataDir(VERSIONING_DATA_ROOT, GEOGIT_INDEX);

        EnvironmentBuilder esb = new EnvironmentBuilder(new EntityStoreConfig());

        Properties bdbEnvProperties = null;
        Environment geogitEnv = esb.buildEnvironment(geogitRepo, bdbEnvProperties);
        Environment indexEnv = esb.buildEnvironment(indexRepo, bdbEnvProperties);

        RepositoryDatabase ggitRepoDb = new JERepositoryDatabase(geogitEnv, indexEnv);

        // RepositoryDatabase ggitRepoDb = new FileSystemRepositoryDatabase(geogitRepo);

        Repository repository = new Repository(ggitRepoDb, this.baseRepoDir);
        repository.create();

        this.geoGit = new GeoGIT(repository);

        // StatsConfig config = new StatsConfig();
        // config.setClear(true);
        // System.err.println(geogitEnvironment.getStats(config));
    }

    public static GEOGIT get() {
        if (singleton == null) {
            return GeoServerExtensions.bean(GEOGIT.class);
        }
        return singleton;
    }

    /**
     * Allows to set a specific GEOGIT instance to be returned by {@link #get()}. WARNING: used for
     * test purposed only.
     * 
     * @param singleton
     */
    public static void set(GEOGIT singleton) {
        GEOGIT.singleton = singleton;
    }

    /**
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
        Repository repository = geoGit.getRepository();
        repository.close();
        singleton = null;
    }

    public List<Name> listLayers() throws Exception {
        geoGit.checkout().setName("master").call();
        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        List<Name> typeNames = workingTree.getFeatureTypeNames();
        return typeNames;
    }

    /**
     * Adds the GeoSever Catalog's FeatureType named after {@code featureTypeName} to the master
     * branch.
     * 
     * @param featureTypeName
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Future<?> initialize(final Name featureTypeName) throws Exception {

        final FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByName(featureTypeName);
        Assert.notNull(featureTypeInfo, "No FeatureType named " + featureTypeName
                + " found in the Catalog");

        final FeatureSource featureSource = featureTypeInfo.getFeatureSource(null, null);
        if (featureSource == null) {
            throw new NullPointerException(featureTypeInfo + " didn't return a FeatureSource");
        }
        if (!(featureSource instanceof FeatureStore)) {
            throw new IllegalArgumentException("Can't version "
                    + featureTypeInfo.getQualifiedName() + " because it is read only");
        }

        ImportVersionedLayerTask importTask;
        importTask = new ImportVersionedLayerTask(featureSource, geoGit);
        LongTaskMonitor monitor = GeoServerExtensions.bean(LongTaskMonitor.class);
        Future<RevCommit> future = monitor.dispatch(importTask);
        return future;
    }

    public boolean isReplicated(final Name featureTypeName) {
        return geoGit.getRepository().getWorkingTree().hasRoot(featureTypeName);
    }

    public void stageRename(final Name typeName, final String oldFid, final String newFid) {

        StagingArea index = geoGit.getRepository().getIndex();

        final String namespaceURI = typeName.getNamespaceURI();
        final String localPart = typeName.getLocalPart();

        List<String> from = Arrays.asList(namespaceURI, localPart, oldFid);
        List<String> to = Arrays.asList(namespaceURI, localPart, newFid);

        index.renamed(from, to);
    }

    public GeoGIT getGeoGit() {
        return this.geoGit;
    }

    public Repository getRepository() {
        return this.geoGit.getRepository();
    }

    public File getBaseRepoDir() {
        return baseRepoDir;
    }

}
