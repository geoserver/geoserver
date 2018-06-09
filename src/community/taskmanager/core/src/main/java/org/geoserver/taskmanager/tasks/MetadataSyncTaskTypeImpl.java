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
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geotools.referencing.CRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetadataSyncTaskTypeImpl implements TaskType {

    public static final String NAME = "MetadataSync";

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_LAYER = "layer";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected GeoServerDataDirectory geoServerDataDirectory;

    @Autowired protected Catalog catalog;

    @Autowired protected ExtTypes extTypes;

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
        GSResourceEncoder re = syncMetadata(resource);
        if (!restManager.getPublisher().configureResource(ws, storeType, storeName, re)) {
            throw new TaskException(
                    "Failed to configure resource " + ws + ":" + resource.getName());
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
                            ? si.getWorkspace() + ":" + si.getName()
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

    protected static GSResourceEncoder syncMetadata(ResourceInfo resource) {
        return syncMetadata(resource, resource.getName());
    }

    protected static GSResourceEncoder syncMetadata(ResourceInfo resource, String name) {
        final GSResourceEncoder re;
        if (resource instanceof CoverageInfo) {
            CoverageInfo coverage = (CoverageInfo) resource;
            final GSCoverageEncoder coverageEncoder = new GSCoverageEncoder();
            for (String format : coverage.getSupportedFormats()) {
                coverageEncoder.addSupportedFormats(format);
            }
            for (String srs : coverage.getRequestSRS()) {
                coverageEncoder.setRequestSRS(srs); // wrong: should be add
            }
            for (String srs : coverage.getResponseSRS()) {
                coverageEncoder.setResponseSRS(srs); // wrong: should be add
            }
            coverageEncoder.setNativeCoverageName(coverage.getNativeCoverageName());
            coverageEncoder.setNativeFormat(coverage.getNativeFormat());
            re = coverageEncoder;
        } else {
            re = new GSFeatureTypeEncoder();
            if (resource.getNativeCRS() != null) {
                re.setNativeCRS(CRS.toSRS(resource.getNativeCRS()));
            }
            re.setNativeName(resource.getNativeName());
        }
        re.setName(name);
        re.setTitle(resource.getTitle());
        re.setAbstract(resource.getAbstract());
        re.setDescription(resource.getAbstract());
        re.setSRS(resource.getSRS());
        for (KeywordInfo ki : resource.getKeywords()) {
            re.addKeyword(ki.getValue(), ki.getLanguage(), ki.getVocabulary());
        }
        for (MetadataLinkInfo mdli : resource.getMetadataLinks()) {
            re.addMetadataLinkInfo(mdli.getType(), mdli.getMetadataType(), mdli.getContent());
        }
        for (Map.Entry<String, Serializable> entry : resource.getMetadata().entrySet()) {
            if (entry.getValue() != null) {
                re.setMetadataString(entry.getKey(), entry.getValue().toString());
            }
        }
        re.setProjectionPolicy(
                resource.getProjectionPolicy() == null
                        ? ProjectionPolicy.NONE
                        : ProjectionPolicy.valueOf(resource.getProjectionPolicy().toString()));
        if (resource.getNativeBoundingBox() != null) {
            re.setNativeBoundingBox(
                    resource.getNativeBoundingBox().getMinX(),
                    resource.getNativeBoundingBox().getMinY(),
                    resource.getNativeBoundingBox().getMaxX(),
                    resource.getNativeBoundingBox().getMaxY(),
                    resource.getSRS());
        }
        if (resource.getLatLonBoundingBox() != null) {
            re.setLatLonBoundingBox(
                    resource.getLatLonBoundingBox().getMinX(),
                    resource.getLatLonBoundingBox().getMinY(),
                    resource.getLatLonBoundingBox().getMaxX(),
                    resource.getLatLonBoundingBox().getMaxY(),
                    resource.getSRS());
        }

        // dimensions, must happen after setName or strange things happen (gs-man bug)
        if (resource instanceof CoverageInfo) {
            CoverageInfo coverage = (CoverageInfo) resource;
            for (CoverageDimensionInfo di : coverage.getDimensions()) {
                ((GSCoverageEncoder) re)
                        .addCoverageDimensionInfo(
                                di.getName(),
                                di.getDescription(),
                                Double.toString(di.getRange().getMinimum()),
                                Double.toString(di.getRange().getMaximum()),
                                di.getUnit(),
                                di.getDimensionType() == null
                                        ? null
                                        : di.getDimensionType().identifier());
            }
        }

        return re;
    }
}
