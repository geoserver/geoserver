/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.CatalogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetadataTemplateSyncTaskTypeImpl implements TaskType {

    public static final String NAME = "MetadataTemplateSync";

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_METADATA_TEMPLATE = "metadata-template";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected ExtTypes extTypes;

    @Autowired protected Catalog catalog;

    @Autowired protected CatalogUtil catalogUtil;

    @Autowired protected MetadataTemplateService templateService;

    private final ParameterType metadataTemplateType =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return templateService.list().stream()
                            .map(t -> t.getName())
                            .collect(Collectors.toList());
                }

                @Override
                public MetadataTemplate parse(String value, List<String> dependsOnRawValues) {
                    return templateService.findByName(value);
                }
            };

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_EXT_GS, new ParameterInfo(PARAM_EXT_GS, extTypes.extGeoserver, true));
        paramInfo.put(
                PARAM_METADATA_TEMPLATE,
                new ParameterInfo(PARAM_METADATA_TEMPLATE, metadataTemplateType, true));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }
        if (!restManager.getReader().existGeoserver()) {
            throw new TaskException("Failed to connect to geoserver " + extGS.getUrl());
        }

        final MetadataTemplate template =
                (MetadataTemplate) ctx.getParameterValues().get(PARAM_METADATA_TEMPLATE);
        List<String> failedLayers = new ArrayList<>();
        for (String resourceId : template.getLinkedLayers()) {
            final ResourceInfo resource = catalog.getResource(resourceId, ResourceInfo.class);
            if (resource != null) {
                final StoreInfo store = resource.getStore();
                final StoreType storeType =
                        store instanceof CoverageStoreInfo
                                ? StoreType.COVERAGESTORES
                                : StoreType.DATASTORES;
                final String ws = store.getWorkspace().getName();
                String storeName;
                RESTLayer restLayer = restManager.getReader().getLayer(ws, resource.getName());
                if (restLayer == null) {
                    failedLayers.add(resource.prefixedName());
                } else {
                    Pattern pattern =
                            Pattern.compile(
                                    "rest/workspaces/"
                                            + ws
                                            + "/"
                                            + storeType.toString()
                                            + "/([^/]*)/");
                    Matcher matcher = pattern.matcher(restLayer.getResourceUrl());
                    if (!matcher.find()) {
                        failedLayers.add(resource.prefixedName());
                    } else {
                        storeName = matcher.group(1);
                        // sync resource
                        GSResourceEncoder re = catalogUtil.syncMetadata(resource);
                        if (!restManager
                                .getPublisher()
                                .configureResource(ws, storeType, storeName, re)) {
                            failedLayers.add(resource.prefixedName());
                        }
                    }
                }
            }
        }
        return new TaskResult() {
            @Override
            public String successMessage() {
                if (failedLayers.size() > 0) {
                    StringBuilder sb =
                            new StringBuilder("The following layers failed to synchronize: ");
                    for (String failedLayer : failedLayers) {
                        sb.append(failedLayer + ", ");
                    }
                    sb.setLength(sb.length() - 2);
                    return sb.toString();
                } else {
                    return null;
                }
            }

            @Override
            public void commit() throws TaskException {
                // do nothing, it's already done
            }

            @Override
            public void rollback() throws TaskException {
                throw new TaskException("Cannot roll-back metadata template synchronisation task");
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
