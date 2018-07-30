/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.api.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.boundlessgeo.gsr.translate.feature.FeatureDAO;
import com.boundlessgeo.gsr.translate.map.LayerDAO;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.geometry.SpatialRelationship;
import com.boundlessgeo.gsr.model.map.LayerNameComparator;
import com.boundlessgeo.gsr.model.map.LayerOrTable;
import com.boundlessgeo.gsr.model.map.MapServiceRoot;

/**
 * Controller for the root Map Service endpoint
 */
@RestController @RequestMapping(path = "/gsr/services/{workspaceName}/MapServer", produces = MediaType
    .APPLICATION_JSON_VALUE)
public class MapServiceController extends AbstractGSRController {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(MapServiceController.class);

    @Autowired
    public MapServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping
    public MapServiceRoot mapServiceGet(@PathVariable String workspaceName) throws IOException {
        WorkspaceInfo workspace = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("Workspace name " + workspaceName + " does not correspond to any workspace.");
        }
        WMSInfo service = geoServer.getService(workspace, WMSInfo.class);
        if (service == null) {
            service = geoServer.getService(WMSInfo.class);
        }
        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : geoServer.getCatalog().getLayers()) {
            if (workspace.equals(l.getResource().getStore().getWorkspace())) {
                layersInWorkspace.add(l);
            }
        }
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);

        return new MapServiceRoot(service, Collections.unmodifiableList(layersInWorkspace));
    }

    @GetMapping(path = {"/{layerId}"})
    public LayerOrTable getLayer(@PathVariable String workspaceName, @PathVariable Integer layerId) throws IOException {
        return LayerDAO.find(catalog, workspaceName, layerId);
    }

    @GetMapping(path = "/identify")
    public IdentifyServiceResult identify(@PathVariable String workspaceName,
        @RequestParam(name = "geometryType", required = false, defaultValue = "esriGeometryPoint") String
            geometryTypeName,
        @RequestParam(name = "geometry", required = false) String geometryText,
        @RequestParam(name = "sr", required = false) String srCode,
        @RequestParam(name = "time", required = false) String time) {

        IdentifyServiceResult result = new IdentifyServiceResult();

        LayerDAO.find(catalog, workspaceName).layers.forEach(layer -> {
            try {
                FeatureCollection collection = FeatureDAO
                    .getFeatureCollectionForLayer(workspaceName, layer.getId(), geometryTypeName, geometryText,
                        srCode, srCode, SpatialRelationship.INTERSECTS.getName(), null, null, time, null, null, null,true,
                        null, layer.layer);

                result.getResults().addAll(IdentifyServiceResult.encode(collection, layer));
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Exception generated getting features for layer: " + layer, e);
                throw new RuntimeException(e);
            }
        });

        return result;
    }

    @GetMapping(path = "/find")
    public IdentifyServiceResult search(@PathVariable String workspaceName, @RequestParam String searchText,
        @RequestParam(required = false, defaultValue = "true") boolean contains,
        @RequestParam(required = false) String searchField,
        @RequestParam(required = false, defaultValue = "true") boolean returnGeometries, @RequestParam String layers) {

        IdentifyServiceResult result = new IdentifyServiceResult();
        Set<String> searchFields = null;

        if (StringUtils.isNotEmpty(searchField)) {
            searchFields = Arrays.stream(searchField.split(",")).collect(Collectors.toSet());
        }

        for (String s : layers.split(",")) {
            Integer layerId = Integer.parseInt(s);
            try {
                LayerOrTable layerOrTable = LayerDAO.find(catalog, workspaceName, layerId);
                if (layerOrTable != null && layerOrTable.layer != null) {
                    FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerOrTable.layer.getResource();
                    FeatureType featureType = featureTypeInfo.getFeatureType();
                    Filter filter = Filter.EXCLUDE;

                    for (PropertyDescriptor propertyDescriptor : featureType.getDescriptors()) {
                        if (searchFields == null || searchFields.contains(propertyDescriptor.getName().toString())) {
                            Class<?> binding = propertyDescriptor.getType().getBinding();
                            if (binding.equals(String.class)) {
                                if (contains) {
                                    filter = FILTERS.or(filter, FILTERS
                                        .like(FILTERS.property(propertyDescriptor.getName()), "%" + searchText + "%",
                                            "%", "?", "\\"));

                                } else {
                                    filter = FILTERS.or(filter, FILTERS
                                        .equal(FILTERS.property(propertyDescriptor.getName()),
                                            FILTERS.literal(searchText)));
                                }
                            }
                        }
                    }
                    Query query = new Query(featureTypeInfo.getName(), filter);
                    FeatureSource source = featureTypeInfo.getFeatureSource(null, null);
                    FeatureCollection features = source.getFeatures(query);
                    result.getResults().addAll(IdentifyServiceResult.encode(features, layerOrTable));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
