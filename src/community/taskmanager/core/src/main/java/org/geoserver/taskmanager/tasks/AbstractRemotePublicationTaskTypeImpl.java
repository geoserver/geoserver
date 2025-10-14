/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.Purge;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.encoder.GSGenericStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskRunnable;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.CatalogUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractRemotePublicationTaskTypeImpl implements TaskType {

    private static final Logger LOGGER = Logging.getLogger(DbRemotePublicationTaskTypeImpl.class);

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_WORKSPACE = "workspace";

    public static final String PARAM_LAYER = "layer";

    protected final Map<String, ParameterInfo> paramInfo = new LinkedHashMap<String, ParameterInfo>();

    @Autowired
    protected CatalogUtil catalogUtil;

    @Autowired
    protected Catalog catalog;

    @Autowired
    protected ExtTypes extTypes;

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_EXT_GS, new ParameterInfo(PARAM_EXT_GS, extTypes.extGeoserver, true));
        ParameterInfo paramWorkspace = new ParameterInfo(PARAM_WORKSPACE, extTypes.workspace, false);
        paramInfo.put(PARAM_WORKSPACE, paramWorkspace);
        paramInfo.put(
                PARAM_LAYER,
                new ParameterInfo(PARAM_LAYER, extTypes.internalLayer, true).dependsOn(false, paramWorkspace));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final LayerInfo layer = (LayerInfo) ctx.getParameterValues().get(PARAM_LAYER);
        final ResourceInfo resource = layer.getResource();
        final StoreInfo store = resource.getStore();
        final StoreType storeType =
                store instanceof CoverageStoreInfo ? StoreType.COVERAGESTORES : StoreType.DATASTORES;
        final String ws = store.getWorkspace().getName();

        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }

        if (!restManager.getReader().existGeoserver()) {
            throw new TaskException("Failed to connect to geoserver " + extGS.getUrl());
        }

        final Set<String> createWorkspaces = new HashSet<String>();
        final Set<StyleInfo> createStyles = new HashSet<StyleInfo>();

        if (!restManager.getReader().existsWorkspace(ws)) {
            createWorkspaces.add(ws);
        }
        Set<StyleInfo> styles = new HashSet<StyleInfo>(layer.getStyles());
        styles.add(layer.getDefaultStyle());
        for (StyleInfo si : styles) {
            if (si != null) {
                String wsStyle = CatalogUtil.wsName(si.getWorkspace());
                if (!restManager.getReader().existsStyle(wsStyle, si.getName())) {
                    createStyles.add(si);
                    if (wsStyle != null && !restManager.getReader().existsWorkspace(wsStyle)) {
                        createWorkspaces.add(wsStyle);
                    }
                }
            }
        }

        final String storeName = getStoreName(store, ctx);
        final boolean existsStore = (storeType == StoreType.DATASTORES
                ? restManager.getReader().existsDatastore(ws, storeName)
                : restManager.getReader().existsCoveragestore(ws, storeName));
        final boolean createStore = neverReuseStore() || !existsStore;
        final boolean existsResource = existsStore && storeType == StoreType.DATASTORES
                ? restManager.getReader().existsFeatureType(ws, storeName, resource.getName())
                : restManager.getReader().existsCoverage(ws, storeName, resource.getName());
        final String tempName = "_temp_" + UUID.randomUUID().toString().replace('-', '_');
        ctx.getBatchContext().put(layer.prefixedName(), resource.getNamespace().getPrefix() + ":" + tempName);
        final String actualStoreName = neverReuseStore() && createStore ? tempName : storeName;

        final GSResourceEncoder finalRe =
                resource instanceof CoverageInfo ? new GSCoverageEncoder(false) : new GSFeatureTypeEncoder(false);
        finalRe.setName(resource.getName());
        finalRe.setAdvertised(true);
        final Finalizer finalizer = new Finalizer(finalRe, new TaskRunnable<GSResourceEncoder>() {
            @Override
            public void run(GSResourceEncoder re) throws TaskException {
                if (!restManager.getPublisher().configureResource(ws, storeType, storeName, tempName, re)) {
                    throw new TaskException("Failed to rename/reconfigure resource "
                            + ws
                            + ":"
                            + tempName
                            + " to "
                            + resource.getName());
                }
                ctx.getBatchContext().delete(layer.prefixedName());
            }
        });

        try {

            for (String newWs : createWorkspaces) { // workspace doesn't exist yet, publish
                LOGGER.log(Level.INFO, "Workspace doesn't exist: " + newWs + " on " + extGS.getName() + ", creating.");
                try {
                    if (!restManager
                            .getPublisher()
                            .createWorkspace(
                                    newWs,
                                    new URI(catalog.getNamespaceByPrefix(newWs).getURI()))) {
                        throw new TaskException("Failed to create workspace " + newWs);
                    }
                } catch (URISyntaxException e) {
                    throw new TaskException("Failed to create workspace " + newWs, e);
                }
            }

            if (createStore) {
                try {
                    if (!createStore(extGS, restManager, store, ctx, actualStoreName)) {
                        throw new TaskException("Failed to create store " + ws + ":" + actualStoreName);
                    }
                } catch (IOException e) {
                    throw new TaskException("Failed to create store " + ws + ":" + actualStoreName, e);
                }
            } else {
                LOGGER.log(
                        Level.INFO, "Store exists: " + storeName + " on " + extGS.getName() + ", skipping creation.");
            }

            // create resource (and layer)

            final GSResourceEncoder re = catalogUtil.syncMetadata(resource, tempName);
            re.setNativeName(resource.getNativeName());
            if (re instanceof GSCoverageEncoder encoder) {
                encoder.setNativeCoverageName(((CoverageInfo) resource).getNativeCoverageName());
            }
            re.setAdvertised(false);

            postProcess(storeType, resource, re, ctx, finalizer);

            // -- disable old store to avoid conflicts
            if (createStore && existsStore) {
                restManager
                        .getStoreManager()
                        .update(
                                ws,
                                storeName,
                                new GSGenericStoreEncoder(storeType, null, null, null, null, null, false));
            }

            // -- resource might have already been created together with store
            if (createStore && storeType == StoreType.DATASTORES
                    ? restManager.getReader().existsFeatureType(ws, actualStoreName, actualStoreName)
                    : restManager.getReader().existsCoverage(ws, actualStoreName, actualStoreName)) {
                if (!restManager
                        .getPublisher()
                        .configureResource(ws, storeType, actualStoreName, actualStoreName, re)) {
                    throw new TaskException("Failed to configure resource " + ws + ":" + re.getName());
                }
            } else {
                if (!restManager.getPublisher().createResource(ws, storeType, actualStoreName, re)) {
                    throw new TaskException("Failed to create resource " + ws + ":" + re.getName());
                }
            }

            for (StyleInfo si : createStyles) { // style doesn't exist yet, publish
                LOGGER.log(
                        Level.INFO, "Style doesn't exist: " + si.getName() + " on " + extGS.getName() + ", creating.");
                if (!restManager
                        .getStyleManager()
                        .publishStyleZippedInWorkspace(
                                CatalogUtil.wsName(si.getWorkspace()),
                                catalogUtil.createStyleZipFile(si),
                                si.getName())) {
                    throw new TaskException("Failed to create style " + si.getName());
                }
                if (!restManager
                        .getStyleManager()
                        .updateStyleInWorkspace(
                                CatalogUtil.wsName(si.getWorkspace()), catalogUtil.syncStyle(si), si.getName())) {
                    throw new TaskException("Failed to sync style " + si.getName());
                }
            }

            // config layer
            final GSLayerEncoder layerEncoder = new GSLayerEncoder();
            catalogUtil.syncMetadata(layer, layerEncoder);
            if (layer.getDefaultStyle() != null) {
                layerEncoder.setDefaultStyle(
                        CatalogUtil.wsName(layer.getDefaultStyle().getWorkspace()),
                        layer.getDefaultStyle().getName());
            }
            for (StyleInfo si : layer.getStyles()) {
                layerEncoder.addStyle(
                        si.getWorkspace() != null
                                ? CatalogUtil.wsName(si.getWorkspace()) + ":" + si.getName()
                                : si.getName());
            }
            if (!restManager.getPublisher().configureLayer(ws, tempName, layerEncoder)) {
                throw new TaskException("Failed to configure layer " + ws + ":" + resource.getName());
            }

        } catch (TaskException e) {
            // clean-up if necessary
            restManager.getPublisher().removeResource(ws, storeType, storeName, tempName);
            if (createStore) {
                restManager.getPublisher().removeStore(ws, actualStoreName, storeType, true, Purge.NONE);
            }
            for (StyleInfo style : createStyles) {
                restManager
                        .getPublisher()
                        .removeStyleInWorkspace(CatalogUtil.wsName(style.getWorkspace()), style.getName(), true);
            }
            for (String createdWs : createWorkspaces) {
                restManager.getPublisher().removeWorkspace(createdWs, true);
            }
            throw e;
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                // remove old resource if exists
                if (existsResource) {
                    if (!restManager.getPublisher().removeLayer(ws, resource.getName())
                            || !restManager
                                    .getPublisher()
                                    .removeResource(ws, storeType, storeName, resource.getName())) {
                        throw new TaskException("Failed to remove old layer " + ws + ":" + resource.getName());
                    }
                }

                if (!actualStoreName.equals(storeName)) {
                    // remove old store if exists
                    if (existsStore) {
                        if (!cleanStore(restManager, store, storeType, storeName, ctx, true)) {
                            throw new TaskException("Failed to remove old store " + ws + ":" + storeName);
                        }
                    }

                    // set proper name store
                    if (!restManager
                            .getStoreManager()
                            .update(
                                    ws,
                                    actualStoreName,
                                    new GSGenericStoreEncoder(storeType, null, null, storeName, null, null))) {
                        throw new TaskException(
                                "Failed to rename store " + ws + ":" + actualStoreName + " to " + storeName);
                    }
                }

                // set proper name resource
                if (finalizer.isFinalizeAtCommit()) {
                    finalizer.run();
                }
            }

            @Override
            public void rollback() throws TaskException {

                if (!restManager.getPublisher().removeLayer(ws, tempName)
                        || !restManager.getPublisher().removeResource(ws, storeType, actualStoreName, tempName)) {
                    throw new TaskException("Failed to remove layer " + ws + ":" + tempName);
                }

                if (createStore) {
                    if (!cleanStore(restManager, store, storeType, actualStoreName, ctx, true)) {
                        throw new TaskException("Failed to remove store " + ws + ":" + actualStoreName);
                    }
                    // -- re-enable old store to avoid conflicts
                    if (existsStore) {
                        restManager
                                .getStoreManager()
                                .update(
                                        ws,
                                        storeName,
                                        new GSGenericStoreEncoder(storeType, null, null, null, null, null, true));
                    }
                }

                for (StyleInfo style : createStyles) {
                    if (!restManager
                            .getPublisher()
                            .removeStyleInWorkspace(CatalogUtil.wsName(style.getWorkspace()), style.getName(), true)) {
                        throw new TaskException("Failed to remove style "
                                + layer.getDefaultStyle().getName());
                    }
                }
                for (String createdWs : createWorkspaces) {
                    if (!restManager.getPublisher().removeWorkspace(createdWs, true)) {
                        throw new TaskException("Failed to remove workspace " + ws);
                    }
                }
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final LayerInfo layer = (LayerInfo) ctx.getParameterValues().get(PARAM_LAYER);
        final ResourceInfo resource = layer.getResource();
        final StoreInfo store = resource.getStore();
        final String storeName = getStoreName(store, ctx);
        final StoreType storeType =
                store instanceof CoverageStoreInfo ? StoreType.COVERAGESTORES : StoreType.DATASTORES;
        final String ws = store.getWorkspace().getName();
        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }
        if (restManager.getReader().existsLayer(ws, layer.getName(), true)) {
            if (!restManager.getPublisher().removeLayer(ws, resource.getName())
                    || !restManager.getPublisher().removeResource(ws, storeType, storeName, resource.getName())) {
                throw new TaskException("Failed to remove layer " + ws + ":" + resource.getName());
            }
            if (!cleanStore(restManager, store, storeType, storeName, ctx, false)) {
                if (neverReuseStore()) {
                    throw new TaskException("Failed to remove store " + ws + ":" + storeName);
                } // else store is still in use
            }
            // will not clean-up style and ws
            // because we don't know if they were created by this task.
        }
    }

    protected abstract boolean createStore(
            ExternalGS extGS, GeoServerRESTManager restManager, StoreInfo store, TaskContext ctx, String name)
            throws IOException, TaskException;

    protected abstract boolean neverReuseStore();

    protected String getStoreName(StoreInfo store, TaskContext ctx) throws TaskException {
        return store.getName();
    }

    protected boolean cleanStore(
            GeoServerRESTManager restManager,
            StoreInfo store,
            StoreType storeType,
            String storeName,
            TaskContext ctx,
            boolean recurse)
            throws TaskException {
        return restManager
                .getPublisher()
                .removeStore(store.getWorkspace().getName(), storeName, storeType, recurse, Purge.NONE);
    }

    protected void postProcess(
            StoreType storeType, ResourceInfo resource, GSResourceEncoder re, TaskContext ctx, Finalizer finalizer)
            throws TaskException {}

    protected class Finalizer {
        private GSResourceEncoder encoder;
        private TaskRunnable<GSResourceEncoder> runnable;
        private boolean finalizeAtCommit = true;

        public Finalizer(GSResourceEncoder encoder, TaskRunnable<GSResourceEncoder> runnable) {
            this.runnable = runnable;
            this.encoder = encoder;
        }

        public boolean isFinalizeAtCommit() {
            return finalizeAtCommit;
        }

        public void setFinalizeAtCommit(boolean finalizeAtCommit) {
            this.finalizeAtCommit = finalizeAtCommit;
        }

        public GSResourceEncoder getEncoder() {
            return encoder;
        }

        public void run() throws TaskException {
            runnable.run(encoder);
        }
    }
}
