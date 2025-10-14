/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.map;

import java.io.IOException;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.FeatureList;
import org.geoserver.gsr.model.map.LayersAndTables;
import org.geoserver.gsr.translate.feature.FeatureDAO;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.gsr.translate.map.LayerDAO;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the Map Service query endpoint */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/MapServer", produces = MediaType.APPLICATION_JSON_VALUE)
public class QueryController extends AbstractGSRController {

    @Autowired
    public QueryController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/{layerId}/query", name = "MapServerQuery")
    public GSRModel query(
            @PathVariable String workspaceName,
            @PathVariable Integer layerId,
            @RequestParam(name = "geometryType", required = false, defaultValue = "esriGeometryEnvelope")
                    String geometryTypeName,
            @RequestParam(name = "geometry", required = false) String geometryText,
            @RequestParam(name = "inSR", required = false) String inSRText,
            @RequestParam(name = "outSR", required = false) String outSRText,
            @RequestParam(name = "spatialRel", required = false, defaultValue = "esriSpatialRelIntersects")
                    String spatialRelText,
            @RequestParam(name = "objectIds", required = false) String objectIdsText,
            @RequestParam(name = "relationPattern", required = false) String relatePattern,
            @RequestParam(name = "time", required = false) String time,
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "maxAllowableOffsets", required = false) String maxAllowableOffsets,
            @RequestParam(name = "where", required = false) String whereClause,
            @RequestParam(name = "returnGeometry", required = false, defaultValue = "true") Boolean returnGeometry,
            @RequestParam(name = "outFields", required = false, defaultValue = "*") String outFieldsText,
            @RequestParam(name = "returnIdsOnly", required = false, defaultValue = "false") boolean returnIdsOnly,
            @RequestParam(name = "quantizationParameters", required = false) String quantizationParameters)
            throws IOException {

        LayersAndTables layersAndTables = LayerDAO.find(catalog, workspaceName);

        FeatureCollection<? extends FeatureType, ? extends Feature> features =
                FeatureDAO.getFeatureCollectionForLayerWithId(
                        workspaceName,
                        layerId,
                        geometryTypeName,
                        geometryText,
                        inSRText,
                        outSRText,
                        spatialRelText,
                        objectIdsText,
                        relatePattern,
                        time,
                        text,
                        maxAllowableOffsets,
                        whereClause,
                        returnGeometry,
                        outFieldsText,
                        layersAndTables);
        if (returnIdsOnly) {
            return FeatureEncoder.objectIds(features);
        } else {
            FeatureList featureList = new FeatureList(features, returnGeometry, outSRText, quantizationParameters);
            return featureList;
        }
    }
}
