/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.wms;

import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.BRANCH;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.DISPLAY_NAME;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.HEAD;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSInfo;
import org.geotools.data.DataStore;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.opengis.filter.Filter;

/**
 * Ensures a global WMS {@link AuthorityURL} exists with name {@code GEOGIG_ENTRY_POINT} and URL
 * {@code http://geogig.org}, and that each {@link LayerInfo layer} from a geogig datastore gets a
 * {@link LayerIdentifierInfo} with authority {@code GEOGIG_ENTRY_POINT} and the identifier composed
 * of {@code <repositoryId>:<nativeName>[:<branch/head>]}
 *
 * <p>The identifier is made of the following parts:
 *
 * <ul>
 *   <li>{@code <repositoryId>}: {@link RepositoryInfo#getId() RepositoryInfo ID} that identifies
 *       the repository referred by the layer's {@link DataStore}. {@link RepositoryInfo}s are
 *       managed by {@link RepositoryManager} and are the way this plugin supports configuring
 *       several datastores against the same repository without duplication of information.
 *   <li>{@code <nativeName>}: the layer's resource {@link ResourceInfo#getNativeName() native name}
 *   <li>{@code <branch/head>}: the geogig datastore's configured {@link
 *       GeoGigDataStoreFactory#BRANCH branch} or {@link GeoGigDataStoreFactory#HEAD}, whichever is
 *       present, or absent if no branch or head is configured (and hence the datastore operates on
 *       whatever the current HEAD is)
 * </ul>
 *
 * <p>Handles the following events:
 *
 * <ul>
 *   <li>{@link WorkspaceInfo} renamed: all geogig layers of stores in that workspace get their
 *       authority URL identifiers updated
 *   <li>{@link DataStoreInfo} renamed: all geogig layers of stores in that workspace get their
 *       authority URL identifiers updated
 *   <li>{@link DataStoreInfo} workspace changed: all geogig layers of stores in that workspace get
 *       their authority URL identifiers updated
 *   <li>{@link LayerInfo} added: if its a geogig layer, creates its geogig authority URL identifier
 *       and saves the layer info
 * </ul>
 */
public class GeogigLayerIntegrationListener implements CatalogListener {

    private static final Logger LOGGER = Logging.getLogger(GeogigLayerIntegrationListener.class);

    public static final String AUTHORITY_URL_NAME = "GEOGIG_ENTRY_POINT";

    public static final String AUTHORITY_URL = "http://geogig.org";

    private final GeoServer geoserver;

    private final GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();

    /** */
    public GeogigLayerIntegrationListener(GeoServer geoserver) {
        LOGGER.log(Level.FINE, "Initialized {0}", getClass().getName());
        this.geoserver = geoserver;
        this.geoserver.getCatalog().addListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (!(event.getSource() instanceof LayerInfo)) {
            return;
        }
        LayerInfo layer = (LayerInfo) event.getSource();
        if (!RepositoryManager.get().isGeogigLayer(layer)) {
            return;
        }
        event.getSource().accept(visitor);
        if (!forceServiceRootLayerHaveGeogigAuthURL()) {
            return;
        }

        setIdentifier(layer);
    }

    private static final ThreadLocal<CatalogInfo> PRE_MOFIFY_EVENT = new ThreadLocal<CatalogInfo>();

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (PRE_MOFIFY_EVENT.get() != null) {
            LOGGER.log(
                    Level.FINE,
                    "pre-modify event exists, ignoring handleModifyEvent ({0})",
                    PRE_MOFIFY_EVENT.get());
            return;
        }
        event.getSource().accept(visitor);

        final CatalogInfo source = event.getSource();
        final boolean isGeogigStore = RepositoryManager.get().isGeogigStore(source);

        boolean tryPostUpdate = (source instanceof WorkspaceInfo) || isGeogigStore;
        final List<String> propertyNames = event.getPropertyNames();
        tryPostUpdate &=
                propertyNames.contains("name") || propertyNames.contains("connectionParameters");

        if (tryPostUpdate) {
            LOGGER.log(Level.FINE, "Storing event for post-handling on {0}", source);
            PRE_MOFIFY_EVENT.set(source);
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        final CatalogInfo preModifySource = PRE_MOFIFY_EVENT.get();
        if (preModifySource == null) {
            return;
        }
        if (!event.getSource().getId().equals(preModifySource.getId())) {
            return;
        }
        PRE_MOFIFY_EVENT.remove();
        LOGGER.log(Level.FINE, "handing post-modify event for {0}", preModifySource);
        event.getSource().accept(visitor);

        CatalogInfo source = event.getSource();

        if (source instanceof WorkspaceInfo) {
            handlePostWorkspaceChange((WorkspaceInfo) source);
        } else if (source instanceof DataStoreInfo) {
            handlePostGeogigStoreChange((DataStoreInfo) source);
        }
    }

    private void handlePostGeogigStoreChange(DataStoreInfo source) {
        Catalog catalog = geoserver.getCatalog();

        final String storeId = source.getId();
        Filter filter = equal("resource.store.id", storeId);

        CloseableIterator<LayerInfo> affectedLayers = catalog.list(LayerInfo.class, filter);
        updateLayers(affectedLayers);
    }

    private void handlePostWorkspaceChange(WorkspaceInfo source) {
        Catalog catalog = geoserver.getCatalog();
        final String wsId = source.getId();
        final String storeType = DISPLAY_NAME;

        Filter filter =
                and(
                        equal("resource.store.workspace.id", wsId),
                        equal("resource.store.type", storeType));

        CloseableIterator<LayerInfo> affectedLayers = catalog.list(LayerInfo.class, filter);
        updateLayers(affectedLayers);
    }

    private void updateLayers(CloseableIterator<LayerInfo> affectedLayers) {
        try {
            while (affectedLayers.hasNext()) {
                LayerInfo geogigLayer = affectedLayers.next();
                setIdentifier(geogigLayer);
            }
        } finally {
            affectedLayers.close();
        }
    }

    private boolean forceServiceRootLayerHaveGeogigAuthURL() {
        LOGGER.fine("Checking for root layer geogig auth URL");

        WMSInfo serviceInfo = geoserver.getService(WMSInfo.class);
        if (serviceInfo == null) {
            LOGGER.info("No WMSInfo available in GeoServer. This is strange but can happen");
            return false;
        }

        List<AuthorityURLInfo> authorityURLs = serviceInfo.getAuthorityURLs();
        for (AuthorityURLInfo urlInfo : authorityURLs) {
            if (AUTHORITY_URL_NAME.equals(urlInfo.getName())) {
                LOGGER.fine("geogig root layer auth URL already exists");
                return true;
            }
        }

        AuthorityURL geogigAuthURL = new AuthorityURL();
        geogigAuthURL.setName(AUTHORITY_URL_NAME);
        geogigAuthURL.setHref(AUTHORITY_URL);
        serviceInfo.getAuthorityURLs().add(geogigAuthURL);

        LOGGER.fine("Saving geogig root layer auth URL");
        geoserver.save(serviceInfo);
        LOGGER.info("geogig root layer auth URL saved");
        return true;
    }

    /** Does nothing */
    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // do nothing
    }

    /** Does nothing */
    @Override
    public void reloaded() {
        // do nothing
    }

    private void setIdentifier(LayerInfo layer) {
        LOGGER.log(
                Level.FINE, "Updating geogig auth identifier for layer {0}", layer.prefixedName());
        final String layerIdentifier = buildLayerIdentifier(layer);
        updateIdentifier(layer, layerIdentifier);
    }

    private void updateIdentifier(LayerInfo geogigLayer, final String newIdentifier) {

        List<LayerIdentifierInfo> layerIdentifiers = geogigLayer.getIdentifiers();
        {
            LayerIdentifierInfo id = null;
            for (LayerIdentifierInfo identifier : layerIdentifiers) {
                if (AUTHORITY_URL_NAME.equals(identifier.getAuthority())) {
                    id = identifier;
                    break;
                }
            }
            if (id != null) {
                if (newIdentifier.equals(id.getIdentifier())) {
                    return;
                }
                layerIdentifiers.remove(id);
            }
        }

        LayerIdentifier newId = new LayerIdentifier();
        newId.setAuthority(AUTHORITY_URL_NAME);
        newId.setIdentifier(newIdentifier);
        layerIdentifiers.add(newId);
        Catalog catalog = geoserver.getCatalog();
        catalog.save(geogigLayer);
        LOGGER.log(
                Level.INFO,
                "Updated geogig auth identifier for layer {0} as {1}",
                new Object[] {geogigLayer.prefixedName(), newIdentifier});
    }

    private String buildLayerIdentifier(LayerInfo geogigLayer) {

        FeatureTypeInfo resource = (FeatureTypeInfo) geogigLayer.getResource();
        DataStoreInfo store = resource.getStore();

        Map<String, Serializable> connectionParameters = store.getConnectionParameters();

        final String repositoryId = (String) connectionParameters.get(REPOSITORY.key);

        Serializable refSpec = connectionParameters.get(BRANCH.key);
        if (refSpec == null) {
            refSpec = connectionParameters.get(HEAD.key);
        }

        StringBuilder identifier =
                new StringBuilder(repositoryId).append(':').append(resource.getNativeName());
        if (refSpec != null) {
            identifier.append(':').append(refSpec);
        }

        return identifier.toString();
    }
}
