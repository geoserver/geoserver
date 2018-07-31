package com.boundlessgeo.gsr.api.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import com.boundlessgeo.gsr.translate.feature.FeatureDAO;
import com.boundlessgeo.gsr.translate.map.LayerDAO;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.api.map.QueryController;
import com.boundlessgeo.gsr.model.feature.FeatureList;
import com.boundlessgeo.gsr.model.feature.FeatureServiceRoot;
import com.boundlessgeo.gsr.model.map.LayerNameComparator;
import com.boundlessgeo.gsr.model.map.LayerOrTable;
import com.boundlessgeo.gsr.model.map.LayersAndTables;

/**
 * Controller for the root Feature Service endpoint
 *
 * Also includes all endpoints from {@link QueryController}
 */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/FeatureServer", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureServiceController extends QueryController {

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
        LayersAndTables layersAndTables = LayerDAO.find(catalog, workspaceName);

        FeatureServiceQueryResult queryResult = new FeatureServiceQueryResult(layersAndTables);

        for (LayerOrTable layerOrTable : layersAndTables.layers) {
            FeatureServiceQueryResult.FeatureLayer layer = new FeatureServiceQueryResult.FeatureLayer(layerOrTable);
            LayerInfo l = layerOrTable.layer;

            if (l.getType() != PublishedType.VECTOR) {
                break;
            }
            FeatureList features = new FeatureList(FeatureDAO.getFeatureCollectionForLayer(workspaceName,
                    layerOrTable.getId(), geometryTypeName, geometryText, inSRText, outSRText, spatialRelText,
                    objectIdsText, relatePattern, time, text, maxAllowableOffsets, whereClause, returnGeometry,
                    outFieldsText, l), returnGeometry, outSRText);
            if (features.features.size() > 0) {
                layer.setFeatures(features);
                queryResult.getLayers().add(layer);
            }
        }

        //TODO: What should returnIdsOnly look like here?
        return queryResult;
    }
}
