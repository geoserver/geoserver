package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.encoder.GSCachedLayerEncoder;
import java.beans.Introspector;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.layer.TileLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigureCachedLayerTaskTypeImpl implements TaskType {

    public static final String NAME = "ConfigureCachedLayer";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_LAYER = "layer";

    @Autowired protected ExtTypes extTypes;

    @Override
    public String getName() {
        return NAME;
    }

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
        final LayerInfo layer =
                (LayerInfo) ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_LAYER));
        String layerName = (String) ctx.getBatchContext().get(layer.prefixedName());
        final GWC gwc = GWC.get();
        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }

        TileLayer tileLayer;
        try {
            tileLayer = gwc.getTileLayerByName(layer.prefixedName());
        } catch (IllegalArgumentException e) {
            tileLayer = null;
        }

        if (tileLayer instanceof GeoServerTileLayer) {
            final GeoServerTileLayerInfo info = ((GeoServerTileLayer) tileLayer).getInfo();
            final GSCachedLayerEncoder cachedLayerEncoder = new GSCachedLayerEncoder();
            cachedLayerEncoder.setName(layerName);
            cachedLayerEncoder.setEnabled(info.isEnabled());
            cachedLayerEncoder.setInMemoryCached(info.isInMemoryCached());
            cachedLayerEncoder.setMetaWidthHeight(info.getMetaTilingX(), info.getMetaTilingY());
            cachedLayerEncoder.setExpireCache(info.getExpireCache());
            cachedLayerEncoder.setExpireClients(info.getExpireClients());
            cachedLayerEncoder.setGutter(info.getGutter());
            cachedLayerEncoder.setBlobStoreId(info.getBlobStoreId());
            for (XMLGridSubset subSet : info.getGridSubsets()) {
                cachedLayerEncoder.addGridSubset(
                        subSet.getGridSetName(),
                        subSet.getZoomStart(),
                        subSet.getZoomStop(),
                        subSet.getMinCachedLevel(),
                        subSet.getMaxCachedLevel());
            }
            for (String mimeFormat : info.getMimeFormats()) {
                cachedLayerEncoder.addMimeFormat(mimeFormat);
            }
            for (ParameterFilter parameterFilter : info.getParameterFilters()) {
                cachedLayerEncoder.addParameterFilter(
                        Introspector.decapitalize(parameterFilter.getClass().getName()),
                        parameterFilter.getKey(),
                        parameterFilter.getDefaultValue());
            }

            if (restManager.getGeoWebCacheRest().getLayer(layerName) == null) {
                if (!restManager.getGeoWebCacheRest().configureLayer(cachedLayerEncoder)) {
                    throw new TaskException("Failed to configure cached layer " + layerName);
                }
            } else {
                if (!restManager.getGeoWebCacheRest().updateLayer(cachedLayerEncoder)) {
                    throw new TaskException("Failed to update cached layer " + layerName);
                }
            }
        } else if (tileLayer == null) {
            if (!restManager.getGeoWebCacheRest().deleteLayer(layerName)) {
                throw new TaskException("Failed to delete cached layer " + layerName);
            }
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                // do nothing, it's already done
            }

            @Override
            public void rollback() throws TaskException {
                throw new TaskException("Cannot roll-back configure cached layer task");
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
}
