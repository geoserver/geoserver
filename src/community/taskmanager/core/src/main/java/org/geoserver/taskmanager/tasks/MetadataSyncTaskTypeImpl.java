/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
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
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.CatalogUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetadataSyncTaskTypeImpl implements TaskType {

    private static final Logger LOGGER = Logging.getLogger(MetadataSyncTaskTypeImpl.class);

    public static final String NAME = "MetadataSync";

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_LAYER = "layer";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected Catalog catalog;

    @Autowired protected ExtTypes extTypes;

    @Autowired protected CatalogUtil catalogUtil;

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_EXT_GS, new ParameterInfo(PARAM_EXT_GS, extTypes.extGeoserver, true));
        paramInfo.put(PARAM_LAYER, new ParameterInfo(PARAM_LAYER, extTypes.internalLayer, true));
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
                store instanceof CoverageStoreInfo
                        ? StoreType.COVERAGESTORES
                        : StoreType.DATASTORES;
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

        RESTLayer restLayer = restManager.getReader().getLayer(ws, layer.getName());

        if (restLayer == null) {
            throw new TaskException("Layer does not exist on destination " + layer.getName());
        }
        String storeName;

        Pattern pattern =
                Pattern.compile("rest/workspaces/" + ws + "/" + storeType.toString() + "/([^/]*)/");
        Matcher matcher = pattern.matcher(restLayer.getResourceUrl());
        if (matcher.find()) {
            storeName = matcher.group(1);
        } else {
            throw new TaskException("Couldn't determine store name for " + layer.getName());
        }
        // sync resource
        GSResourceEncoder re = catalogUtil.syncMetadata(resource);
        if (!restManager.getPublisher().configureResource(ws, storeType, storeName, re)) {
            throw new TaskException(
                    "Failed to configure resource " + ws + ":" + resource.getName());
        }

        // sync styles
        final Set<String> createWorkspaces = new HashSet<String>();
        final Set<StyleInfo> styles = new HashSet<StyleInfo>(layer.getStyles());
        styles.add(layer.getDefaultStyle());
        for (StyleInfo si : styles) {
            if (si != null) {
                String wsStyle = CatalogUtil.wsName(si.getWorkspace());
                if (!restManager.getReader().existsStyle(wsStyle, si.getName())) {
                    if (wsStyle != null && !restManager.getReader().existsWorkspace(wsStyle)) {
                        createWorkspaces.add(wsStyle);
                    }
                }
            }
        }
        for (String newWs : createWorkspaces) { // workspace doesn't exist yet, publish
            LOGGER.log(
                    Level.INFO,
                    "Workspace doesn't exist: " + newWs + " on " + extGS.getName() + ", creating.");
            try {
                if (!restManager
                        .getPublisher()
                        .createWorkspace(
                                newWs, new URI(catalog.getNamespaceByPrefix(newWs).getURI()))) {
                    throw new TaskException("Failed to create workspace " + newWs);
                }
            } catch (URISyntaxException e) {
                throw new TaskException("Failed to create workspace " + newWs, e);
            }
        }

        for (StyleInfo si : styles) {
            LOGGER.log(Level.INFO, "Synchronizing style : " + si.getName());
            String wsName = CatalogUtil.wsName(si.getWorkspace());
            if (!(restManager.getStyleManager().existsStyle(wsName, si.getName())
                    ? restManager
                            .getStyleManager()
                            .updateStyleZippedInWorkspace(
                                    wsName, catalogUtil.createStyleZipFile(si), si.getName())
                    : restManager
                            .getStyleManager()
                            .publishStyleZippedInWorkspace(
                                    wsName, catalogUtil.createStyleZipFile(si), si.getName()))) {
                throw new TaskException("Failed to create style " + si.getName());
            }
        }

        // sync layer
        final GSLayerEncoder layerEncoder = new GSLayerEncoder();
        layerEncoder.setDefaultStyle(
                layer.getDefaultStyle().getWorkspace() == null
                        ? null
                        : layer.getDefaultStyle().getWorkspace().getName(),
                layer.getDefaultStyle().getName());
        for (StyleInfo si : layer.getStyles()) {
            layerEncoder.addStyle(
                    si.getWorkspace() != null
                            ? CatalogUtil.wsName(si.getWorkspace()) + ":" + si.getName()
                            : si.getName());
        }

        if (!restManager.getPublisher().configureLayer(ws, layer.getName(), layerEncoder)) {
            throw new TaskException("Failed to configure layer " + ws + ":" + resource.getName());
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                // do nothing, it's already done
            }

            @Override
            public void rollback() throws TaskException {
                throw new TaskException("Cannot roll-back metadata synchronisation task");
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        throw new TaskException("unsupported");
    }

    @Override
    public boolean supportsCleanup() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
