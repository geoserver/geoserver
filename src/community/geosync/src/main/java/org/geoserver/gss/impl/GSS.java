/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.geogit.api.GeoGIT;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.gss.GeoSyncDataStoreFactory;
import org.geoserver.data.gss.ServerSubscription;
import org.geoserver.geogit.GEOGIT;
import org.geoserver.gss.config.GSSInfo;
import org.geoserver.gss.impl.query.FeedResponse;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.gss.internal.storage.GeoSyncDatabase;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.springframework.beans.factory.DisposableBean;

import com.sleepycat.je.Environment;

/**
 * Service facade to GeoServer
 * 
 * @author Gabriel Roldan
 * 
 */
public class GSS implements DisposableBean {

    private static final Logger LOGGER = Logging.getLogger(GSS.class);

    private static final String GSS_REPO = "gss_repo";

    private final Catalog catalog;

    private final GeoSyncDatabase gssDb;

    private final GEOGIT geogitFacade;

    public GSS(final GEOGIT geogitFacade, final Catalog catalog) throws IOException {
        this.geogitFacade = geogitFacade;
        this.catalog = catalog;

        EnvironmentBuilder esb = new EnvironmentBuilder(new EntityStoreConfig());
        Properties bdbEnvProperties = null;

        final File repoBase = geogitFacade.getBaseRepoDir();
        final File gssRepo = new File(repoBase, GSS_REPO);
        gssRepo.mkdirs();
        Environment gssEnvironment = esb.buildEnvironment(gssRepo, bdbEnvProperties);
        gssDb = new GeoSyncDatabase(gssEnvironment);
        gssDb.create();
    }

    public GeoSyncDatabase getDatabase() {
        return gssDb;
    }

    public List<ServerSubscription> getServerSubscriptions(final boolean enabledOnly) {
        List<DataStoreInfo> dataStores = catalog.getDataStores();
        List<ServerSubscription> subscriptions = new LinkedList<ServerSubscription>();
        for (DataStoreInfo ds : dataStores) {
            if (enabledOnly && !ds.isEnabled()) {
                continue;
            }
            Map<String, Serializable> connectionParameters = ds.getConnectionParameters();
            if (connectionParameters.containsKey(GeoSyncDataStoreFactory.GSS_CAPABILITIES_URL.key)) {
                ServerSubscription s;
                try {
                    s = GeoSyncDataStoreFactory.createSubscription(connectionParameters);
                    subscriptions.add(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return subscriptions;
    }

    public static GSS get() {
        GSS singleton = GeoServerExtensions.bean(GSS.class);
        return singleton;
    }

    /**
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
        gssDb.close();
    }

    public List<Name> listLayers() throws Exception {
        return geogitFacade.listLayers();
    }

    /**
     * Adds the GeoSever Catalog's FeatureType named after {@code featureTypeName} to the master
     * branch.
     * 
     * @param featureTypeName
     * @return
     * @throws Exception
     */
    public Future<?> initialize(final Name featureTypeName) throws Exception {
        return geogitFacade.initialize(featureTypeName);
    }

    public boolean isReplicated(final Name featureTypeName) {
        return geogitFacade.isReplicated(featureTypeName);
    }

    public GSSInfo getGssInfo() {
        return GeoServerExtensions.bean(GeoServer.class).getService(GSSInfo.class);
    }

    public GeoGIT getGeoGit() {
        return geogitFacade.getGeoGit();
    }

    /**
     * <p>
     * The response of a query to the {@code REPLICATIONFEED} is comprised of a single
     * {@code <atom:entry>} per {@link Feature} between (<i>to points in time</i> CORRECTION: every
     * two consecutive commits or changes).<br>
     * <p>
     * Each {@code entry} represents either the addition, deletion, or modification of a single
     * {@code Feature}.
     * <p>
     * The replication feed is mapped to the GeoGit's "master" branch. I.e. it represents the
     * current state of a dataset.
     * 
     * 
     * @param searchTerms
     * @param filter
     * @param startPosition
     * @param maxEntries
     * @return
     * @throws Exception
     * @see {@link DiffEntryListBuilder}
     */
    public FeedImpl queryReplicationFeed(final List<String> searchTerms, final Filter filter,
            final Long startPosition, final Long maxEntries, final SortOrder sortOrder)
            throws ServiceException {

        GeoGIT geoGit = getGeoGit();
        DiffEntryListBuilder diffEntryListBuilder = new DiffEntryListBuilder(this, geoGit);
        diffEntryListBuilder.setSearchTerms(searchTerms);
        diffEntryListBuilder.setFilter(filter);
        diffEntryListBuilder.setStartPosition(startPosition);
        diffEntryListBuilder.setMaxEntries(maxEntries);
        diffEntryListBuilder.setSortOrder(sortOrder);

        FeedImpl feed;
        try {
            feed = diffEntryListBuilder.buildFeed();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return feed;
    }

    public FeedImpl queryChangeFeed(List<String> searchTerms, Filter filter, Long startPosition,
            Long maxEntries, SortOrder sortOrder) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @param searchTerms
     * @param filter
     * @param startPosition
     * @param maxEntries
     * @param sortOrder
     * @return
     */
    public FeedImpl queryResolutionFeed(final List<String> searchTerms, final Filter filter,
            final Long startPosition, final Long maxEntries, final SortOrder sortOrder) {

        GeoGIT geoGit = getGeoGit();
        CommitsEntryListBuilder commitEntryListBuilder = new CommitsEntryListBuilder(this, geoGit);
        commitEntryListBuilder.setSearchTerms(searchTerms);
        commitEntryListBuilder.setFilter(filter);
        commitEntryListBuilder.setStartPosition(startPosition);
        commitEntryListBuilder.setMaxEntries(maxEntries);
        commitEntryListBuilder.setSortOrder(sortOrder);

        FeedImpl feed;
        try {
            feed = commitEntryListBuilder.buildFeed();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return feed;
    }

    public FeatureType getFeatureType(String namespace, String typeName) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(namespace, typeName);
        try {
            return featureType.getFeatureType();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * @return the list of supported {@code GetEntries} output formats by inspecting the application
     *         context for instances of {@link FeedResponse}
     */
    public Set<String> getGetEntriesOutputFormats() {
        final List<FeedResponse> getEntriesResponses;
        getEntriesResponses = GeoServerExtensions.extensions(FeedResponse.class);

        Set<String> supportedFormats = new HashSet<String>();
        for (FeedResponse r : getEntriesResponses) {
            supportedFormats.addAll(r.getOutputFormats());
        }

        return supportedFormats;
    }

    public Catalog getCatalog() {
        return catalog;
    }

}
