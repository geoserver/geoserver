package com.boundlessgeo.gsr.api.feature;

import java.io.IOException;



import com.boundlessgeo.gsr.model.feature.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.boundlessgeo.gsr.translate.feature.FeatureDAO;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;

import com.boundlessgeo.gsr.model.feature.EditResults;
import com.boundlessgeo.gsr.model.feature.Feature;
import com.boundlessgeo.gsr.model.feature.FeatureArray;

import com.boundlessgeo.gsr.translate.map.LayerDAO;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.map.LayerOrTable;

/**
 * Controller for the Feature Service layer endpoint
 */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/FeatureServer", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureLayerController extends AbstractGSRController {

    @Autowired
    public FeatureLayerController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @ResponseBody
    @GetMapping(path = "/{layerId}")
    public FeatureLayer featureGet(@PathVariable String workspaceName, @PathVariable Integer layerId) throws IOException {
        LayerOrTable entry;
        try {
            entry = LayerDAO.find(catalog, workspaceName, layerId);
        } catch (IOException e) {
            throw new NoSuchElementException("Unavailable table or layer in workspace \"" + workspaceName + "\" for id " + layerId + ":" + e);
        }
        if (entry == null) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspaceName + "\" for id " + layerId);
        }

        return new FeatureLayer(entry);
    }

    /**
     *
     * @param workspaceName Workspace name
     * @param layerId Layer id
     * @param objectIdsText A comma delimited list of object ids of features to delete.  If present all other query parameters are ignored.
     * @param geometryTypeName If using a geometry to select delete features, specifies the input geometry type.  Values:
     *      *                    esriGeometryPoint | esriGeometryMultipoint | esriGeometryPolyline | esriGeometryPolygon |
     *      *                    esriGeometryEnvelope
     * @param whereClause If not using a list of object ids or a geometry query use this to pass in an attribute query.  Eg POP2000 > 350000
     * @param geometryText A geometry representing a spatial filter to filter the features by.  See https://developers.arcgis.com/documentation/common-data-types/geometry-objects.htm
     * @param inSRText The spatial reference of the input geometry. If the inSR is not specified, the geometry is
     *      *                   assumed to be in the spatial reference of the map.
     * @param spatialRelText The spatial relationship to be applied on the input geometry while performing the query.
     *      *                   Values: esriSpatialRelIntersects | esriSpatialRelContains | esriSpatialRelCrosses |
     *      *                   esriSpatialRelEnvelopeIntersects | esriSpatialRelIndexIntersects | esriSpatialRelOverlaps |
     *      *                   esriSpatialRelTouches | esriSpatialRelWithin
     * @return
     */
    @ResponseBody
    @PostMapping(path = "/{layerId}/deleteFeatures")
    public EditResults featureDelete(@PathVariable String workspaceName, @PathVariable Integer layerId,
                                     @RequestParam(name = "objectIds", required = false) String objectIdsText,
                                     @RequestParam(name = "geometryType", required = false) String geometryTypeName,
                                     @RequestParam(name = "where", required = false) String whereClause,
                                     @RequestParam(name = "geometry", required = false) String geometryText,
                                     @RequestParam(name = "inSR", required = false) String inSRText,
                                     @RequestParam(name = "spatialRel", required = false) String
                                                 spatialRelText) throws IOException {
        LayerOrTable entry;
        List<EditResult> editResults;
        Long id;

        entry = LayerDAO.find(catalog, workspaceName, layerId);
        if (entry == null) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspaceName + "\" for id " + layerId);
        }
        LayerInfo l = entry.layer;
        FeatureCollection features = FeatureDAO.getFeatureCollectionForLayer(workspaceName,
                entry.getId(), geometryTypeName, geometryText, inSRText, null, spatialRelText,
                objectIdsText, null, null, null, null, whereClause, false,
                null, l);
        long[] ids = FeatureEncoder.objectIds(features).getObjectIds();
        List<Long> idsList = Arrays.stream(ids).boxed().collect(Collectors.toList());
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) l.getResource();
        editResults = FeatureDAO.deleteFeatures(featureTypeInfo, idsList);


        return new EditResults(null, null, editResults);
    }

    /**
     * Update Feature endpoint
     * 
     * @param featureArray Array of features to update
     * @param workspaceName Workspace name
     * @param layerId layer id
     *
     * TODO: Unsupported parameters
     * f - only json supported (used by default), ignored
     * gdbVersion - GSR does not support versioned data, ignored.
     * returnEditMoment - not currently supported, TODO
     * rollbackOnFailure - not currently supported, TODO
     * trueCurveClient - GSR does not support true curve encoding, ignored.
     *
     * @return Results of the update
     * @throws IOException
     */

    @PostMapping(path = "/{layerId}/updateFeatures")
    public EditResults updateFeatures(@RequestBody FeatureArray featureArray, @PathVariable String workspaceName, @PathVariable Integer layerId) throws IOException {
        List<Feature> features = featureArray == null ? null : featureArray.features;
        if (features == null || features.size() < 1) {
            throw new IllegalArgumentException("No features provided");
        }

        LayerInfo layer = featureGet(workspaceName, layerId).layer;

        if (layer.getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();

            return new EditResults(null, FeatureDAO.updateFeatures(fti, features), null);
        } else {
            throw new IllegalArgumentException("Layer is not a feature layer");
        }


    }

    /**
     *
     * @param featureArray Array of features to update
     * @param workspaceName Workspace name
     * @param layerId layer id
     * @return Results of the update
     * @throws IOException
     */
    @PostMapping(path = "/{layerId}/addFeatures")
    public EditResults addFeatures(@RequestBody FeatureArray featureArray,
                                   @PathVariable String workspaceName,
                                   @PathVariable Integer layerId) throws IOException {

        List<Feature> features = featureArray == null ? null : featureArray.features;
        if (features == null || features.size() < 1) {
            throw new IllegalArgumentException("No features provided");
        }

        LayerInfo layer = featureGet(workspaceName, layerId).layer;

        if (layer.getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();

            return new EditResults(FeatureDAO.createFeatures(fti, features),null, null);
        } else {
            throw new IllegalArgumentException("Layer is not a feature layer");
        }


    }
}
