package com.boundlessgeo.gsr.api.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.api.map.QueryController;
import com.boundlessgeo.gsr.core.feature.FeatureList;
import com.boundlessgeo.gsr.core.feature.FeatureServiceRoot;
import com.boundlessgeo.gsr.core.geometry.SpatialRelationship;
import com.boundlessgeo.gsr.core.map.LayerNameComparator;
import com.boundlessgeo.gsr.core.map.LayerOrTable;
import com.boundlessgeo.gsr.core.map.LayersAndTables;

/**
 * Controller for the root Feature Service endpoint
 */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/FeatureServer", produces = MediaType.APPLICATION_JSON_VALUE) public class FeatureServiceController
    extends QueryController {

    @Autowired
    public FeatureServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping
    public FeatureServiceRoot featureServiceGet(@PathVariable String workspaceName) {

        WorkspaceInfo workspace = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("Workspace name " + workspaceName + " does not correspond to any workspace.");
        }
        WFSInfo service = geoServer.getService(workspace, WFSInfo.class);
        if (service == null) {
            service = geoServer.getService(WFSInfo.class);
        }
        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : geoServer.getCatalog().getLayers()) {
            if (l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace().equals(workspace)) {
                layersInWorkspace.add(l);
            }
        }
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);
        return new FeatureServiceRoot(service, Collections.unmodifiableList(layersInWorkspace));
    }

    @GetMapping(path = { "/query" })
    public FeatureServiceQueryResult query(@PathVariable String workspaceName,
        @RequestParam(name = "geometryType", required = false) String geometryTypeName,
        @RequestParam(name = "geometry", required = false) String geometryText,
        @RequestParam(name = "inSR", required = false) String inSRText,
        @RequestParam(name = "outSR", required = false) String outSRText,
        @RequestParam(name = "spatialRel", required = false) String
            spatialRelText,
        @RequestParam(name = "objectIds", required = false) String objectIdsText,
        @RequestParam(name = "relationPattern", required = false) String relatePattern,
        @RequestParam(name = "time", required = false) String time,
        @RequestParam(name = "text", required = false) String text,
        @RequestParam(name = "maxAllowableOffsets", required = false) String maxAllowableOffsets,
        @RequestParam(name = "where", required = false) String whereClause,
        @RequestParam(name = "returnGeometry", required = false, defaultValue = "true") Boolean returnGeometry,
        @RequestParam(name = "outFields", required = false, defaultValue = "*") String outFieldsText,
        @RequestParam(name = "returnIdsOnly", required = false, defaultValue = "false") boolean returnIdsOnly)
        throws IOException {
        LayersAndTables layersAndTables = LayersAndTables.find(catalog, workspaceName);

        FeatureServiceQueryResult queryResult = new FeatureServiceQueryResult(layersAndTables);

        for (LayerOrTable layerOrTable : layersAndTables.layers) {
            FeatureServiceQueryResult.FeatureLayer layer = new FeatureServiceQueryResult.FeatureLayer(layerOrTable);
            LayerInfo l = layerOrTable.layer;

            if (l.getType() != PublishedType.VECTOR) {
                break;
            }

            FeatureTypeInfo featureType = (FeatureTypeInfo) l.getResource();

            final String geometryProperty;
            final String temporalProperty;
            final CoordinateReferenceSystem nativeCRS;

            try {
                GeometryDescriptor geometryDescriptor = featureType.getFeatureType().getGeometryDescriptor();
                nativeCRS = geometryDescriptor.getCoordinateReferenceSystem();
                geometryProperty = geometryDescriptor.getName().getLocalPart();
                DimensionInfo timeInfo = featureType.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo == null || !timeInfo.isEnabled()) {
                    temporalProperty = null;
                } else {
                    temporalProperty = timeInfo.getAttribute();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to determine geometry type for query request");
            }

            //Query Parameters
            final CoordinateReferenceSystem outSR = parseSpatialReference(outSRText);

            SpatialRelationship spatialRel = null;
            if (StringUtils.isNotEmpty(spatialRelText)) {
                spatialRel = SpatialRelationship.fromRequestString(spatialRelText);
            }
            Filter objectIdFilter = parseObjectIdFilter(objectIdsText);
            Filter filter = Filter.INCLUDE;

            final CoordinateReferenceSystem inSR = parseSpatialReference(inSRText, geometryText);
            if (StringUtils.isNotEmpty(geometryText)) {
                filter = buildGeometryFilter(geometryTypeName, geometryProperty, geometryText, spatialRel,
                    relatePattern, inSR, nativeCRS);
            }

            if (time != null) {
                filter = FILTERS.and(filter, parseTemporalFilter(temporalProperty, time));
            }
            if (text != null) {
                throw new UnsupportedOperationException("Text filter not implemented");
            }
            if (maxAllowableOffsets != null) {
                throw new UnsupportedOperationException(
                    "Generalization (via 'maxAllowableOffsets' parameter) not implemented");
            }
            if (whereClause != null) {
                final Filter whereFilter;
                try {
                    whereFilter = ECQL.toFilter(whereClause);
                } catch (CQLException e) {
                    throw new IllegalArgumentException("'where' parameter must be valid CQL; was " + whereClause, e);
                }
                List<Filter> children = Arrays.asList(filter, whereFilter, objectIdFilter);
                filter = FILTERS.and(children);
            }
            String[] properties = parseOutFields(outFieldsText);

            FeatureSource<? extends FeatureType, ? extends Feature> source = featureType.getFeatureSource(null, null);
            final String[] effectiveProperties = adjustProperties(returnGeometry, properties, source.getSchema());

            final Query query;
            if (effectiveProperties == null) {
                query = new Query(featureType.getName(), filter);
            } else {
                query = new Query(featureType.getName(), filter, effectiveProperties);
            }
            query.setCoordinateSystemReproject(outSR);
            FeatureList features = new FeatureList(source.getFeatures(query), true);
            if (features.features.size() > 0) {
                layer.setFeatures(features);
                queryResult.getLayers().add(layer);
            }
        }

        return queryResult;
    }
}
