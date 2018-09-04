package com.boundlessgeo.gsr.api.feature;

import java.io.IOException;


import com.boundlessgeo.gsr.api.GeoServicesExceptionResolver;
import com.boundlessgeo.gsr.api.GeoServicesJacksonJsonConverter;
import com.boundlessgeo.gsr.api.ServiceException;
import com.boundlessgeo.gsr.model.feature.*;


import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.boundlessgeo.gsr.translate.feature.FeatureDAO;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;

import com.boundlessgeo.gsr.model.feature.EditResults;
import com.boundlessgeo.gsr.model.feature.Feature;
import com.boundlessgeo.gsr.model.feature.FeatureArray;

import com.boundlessgeo.gsr.translate.feature.LayerEditsEncoder;
import com.boundlessgeo.gsr.translate.map.LayerDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.*;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.map.LayerOrTable;

import javax.servlet.ServletRequest;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

/**
 * Controller for the Feature Service layer endpoint
 */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/FeatureServer", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureLayerController extends AbstractGSRController {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(FeatureLayerController.class);


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
     * @param rollbackOnFailure Optional parameter to specify if the edits should be applied only if all submitted
     *                          edits succeed. If false, the server will apply the edits that succeed even if some of
     *                          the submitted edits fail. If true, the server will apply the edits only if all edits
     *                          succeed. The default value is true.
     * @param returnEditMoment Optional parameter specifying whether the response will report the time features were
     *                         updated. If returnEditMoment = true, the server will report the time in the response's
     *                         editMoment key. The default value is false.
     * @return
     */
    @ResponseBody
    @PostMapping(path = "/{layerId}/deleteFeatures", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public EditResults featureDelete(
            @PathVariable String workspaceName, @PathVariable Integer layerId,
            @RequestParam(name = "objectIds", required = false) String objectIdsText,
            @RequestParam(name = "geometryType", required = false) String geometryTypeName,
            @RequestParam(name = "where", required = false) String whereClause,
            @RequestParam(name = "geometry", required = false) String geometryText,
            @RequestParam(name = "inSR", required = false) String inSRText,
            @RequestParam(name = "spatialRel", required = false) String spatialRelText,
            @RequestParam(name = "rollbackOnFailure", required = false, defaultValue = "false") boolean rollbackOnFailure,
            @RequestParam(name = "returnEditMoment", required = false, defaultValue = "false") boolean returnEditMoment
    ) throws IOException, ServiceException {

        return deleteFeatures(workspaceName, layerId, objectIdsText, geometryTypeName, whereClause, geometryText, inSRText, spatialRelText, rollbackOnFailure, returnEditMoment);
    }

    /**
     * @See FeatureLayerController#featureDelete
     */
    private EditResults deleteFeatures(String workspaceName, Integer layerId, String objectIdsText, String geometryTypeName, String whereClause, String geometryText, String inSRText, String spatialRelText, boolean rollbackOnFailure, boolean returnEditMoment) throws IOException, ServiceException {
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
        return FeatureDAO.deleteFeatures(featureTypeInfo, idsList, returnEditMoment, rollbackOnFailure);
    }

    /**
     * @See FeatureLayerController#featureDelete
     */
    private EditResults deleteFeatures(String workspaceName, Integer layerId, String objectIdsText,boolean rollbackOnFailure,boolean returnEditMoment) throws IOException,ServiceException{
        return deleteFeatures(workspaceName,layerId,objectIdsText,null,null,null,null,null,rollbackOnFailure,returnEditMoment);
    }

    /**
     * Update Feature endpoint
     *
     * @param features Array of features to update
     * @param workspaceName Workspace name
     * @param layerId layer id
     * @param rollbackOnFailure Optional parameter to specify if the edits should be applied only if all submitted
     *                          edits succeed. If false, the server will apply the edits that succeed even if some of
     *                          the submitted edits fail. If true, the server will apply the edits only if all edits
     *                          succeed. The default value is true.
     * @param returnEditMoment Optional parameter specifying whether the response will report the time features were
     *                         updated. If returnEditMoment = true, the server will report the time in the response's
     *                         editMoment key. The default value is false.
     * TODO: Unsupported parameters
     * f - only json supported (used by default), ignored
     * gdbVersion - GSR does not support versioned data, ignored.
     * trueCurveClient - GSR does not support true curve encoding, ignored.
     *
     * @return Results of the update
     * @throws IOException
     */
    @PostMapping(path = "/{layerId}/updateFeatures", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public EditResults updateFeaturesPost(
            @PathVariable String workspaceName, @PathVariable Integer layerId,
            @RequestParam String features,
            @RequestParam(name = "rollbackOnFailure", required = false, defaultValue = "false") boolean rollbackOnFailure,
            @RequestParam(name = "returnEditMoment", required = false, defaultValue = "false") boolean returnEditMoment
    ) throws IOException, ServiceException {
        FeatureArray featureArray = jsonStringToFeatureArray(features);
        return updateFeatures(featureArray, workspaceName, layerId, rollbackOnFailure, returnEditMoment);


    }

    /**
     * @See FeatureLayerController#updateFeaturesPost
     */
    private EditResults updateFeatures(FeatureArray featureArray, String workspaceName, Integer layerId, boolean rollbackOnFailure, boolean returnEditMoment) throws IOException, ServiceException {
        List<Feature> features = featureArray == null ? null : featureArray.features;
        if (features == null || features.size() < 1) {
            throw new IllegalArgumentException("No features provided");
        }

        LayerInfo layer = featureGet(workspaceName, layerId).layer;

        if (layer.getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();

            return FeatureDAO.updateFeatures(fti, features, returnEditMoment, rollbackOnFailure);
        } else {
            throw new IllegalArgumentException("Layer is not a feature layer");
        }
    }

    /**
     *
     * @param features Array of features to update
     * @param workspaceName Workspace name
     * @param layerId layer id
     * @param rollbackOnFailure Optional parameter to specify if the edits should be applied only if all submitted
     *                          edits succeed. If false, the server will apply the edits that succeed even if some of
     *                          the submitted edits fail. If true, the server will apply the edits only if all edits
     *                          succeed. The default value is true.
     * @param returnEditMoment Optional parameter specifying whether the response will report the time features were
     *                         updated. If returnEditMoment = true, the server will report the time in the response's
     *                         editMoment key. The default value is false.
     * @return Results of the update
     * @throws IOException
     */
    @PostMapping(path = "/{layerId}/addFeatures", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public EditResults addFeaturesPost(
            @PathVariable String workspaceName,
            @PathVariable Integer layerId,
            @RequestParam String features,
            @RequestParam(name = "rollbackOnFailure", required = false, defaultValue = "false") boolean rollbackOnFailure,
            @RequestParam(name = "returnEditMoment", required = false, defaultValue = "false") boolean returnEditMoment
    ) throws IOException, ServiceException {
        FeatureArray featureArray = jsonStringToFeatureArray(features);
        return addFeatures(featureArray, workspaceName, layerId, rollbackOnFailure, returnEditMoment);


    }

    /**
     * @See FeatureLayerController#addFeaturesPost
     */
    private EditResults addFeatures(FeatureArray featureArray, String workspaceName, Integer layerId, boolean rollbackOnFailure, boolean returnEditMoment) throws IOException, ServiceException {
        List<Feature> features = featureArray == null ? null : featureArray.features;
        if (features == null || features.size() < 1) {
            throw new IllegalArgumentException("No features provided");
        }

        LayerInfo layer = featureGet(workspaceName, layerId).layer;

        if (layer.getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();

            return FeatureDAO.createFeatures(fti, features, returnEditMoment, rollbackOnFailure);
        } else {
            throw new IllegalArgumentException("Layer is not a feature layer");
        }
    }

    /**
     *
     * @param adds Array of features to add
     * @param updates Array of features to update
     * @param deletes Comma delimited list of feature ids
     * @param workspaceName Workspace name
     * @param layerId Layer Id
     * @param rollbackOnFailure Optional parameter to specify if the edits should be applied only if all submitted
     *                          edits succeed. If false, the server will apply the edits that succeed even if some of
     *                          the submitted edits fail. If true, the server will apply the edits only if all edits
     *                          succeed. The default value is true.
     * @param returnEditMoment Optional parameter specifying whether the response will report the time features were
     *                         updated. If returnEditMoment = true, the server will report the time in the response's
     *                         editMoment key. The default value is false.
     *
     * TODO: Unsupported parameters
     * f - only json supported (used by default), ignored
     * gdbVersion - GSR does not support versioned data, ignored.
     * trueCurveClient - GSR does not support true curve encoding, ignored.
     * useGlobalIds - GSR does not support global ids, ignored.
     * attachments - GSR does not support attachments, ignored.
     * sessionID - GSR does not support session ids, ignored.
     * usePreviousEditMoment - GSR does not support merging of transactions with the editMoment, ignored.
     *
     * @return Results of adds, updates, and/or deletes
     * @throws IOException
     */
    @PostMapping(path="/{layerId}/applyEdits", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public EditResults applyEditsByLayer(@PathVariable String workspaceName,
                                         @PathVariable Integer layerId,
                                         @RequestParam(name="adds", required=false) String adds,
                                         @RequestParam(name="updates", required=false) String updates,
                                         @RequestParam (name="deletes", required = false)String deletes,
                                         @RequestParam(name = "rollbackOnFailure", required = false, defaultValue = "false") boolean rollbackOnFailure,
                                         @RequestParam(name = "returnEditMoment", required = false, defaultValue = "false") boolean returnEditMoment
                                         ) throws IOException, ServiceException {


        EditResults addEditResults=null;
        EditResults updateEditResults=null;
        EditResults deleteEditResults=null;

        if (adds != null) {
            FeatureArray addsArray = jsonStringToFeatureArray(adds);
            if (addsArray.features != null && addsArray.features.size() > 0) {
                addEditResults = addFeatures(addsArray, workspaceName, layerId, returnEditMoment, rollbackOnFailure);
            }
        }

        if(updates!=null) {
            FeatureArray updatesArray = jsonStringToFeatureArray(updates);
            if (updatesArray != null && updatesArray.features != null && updatesArray.features.size() > 0) {
                updateEditResults = updateFeatures(updatesArray, workspaceName, layerId, returnEditMoment, rollbackOnFailure);

            }
        }

        if(deletes!=null&&deletes.length()>0){
            deleteEditResults = deleteFeatures(workspaceName,layerId,deletes, returnEditMoment, rollbackOnFailure);

        }

        return new EditResults(
                addEditResults!=null?addEditResults.addResults:null,
                updateEditResults!=null?updateEditResults.updateResults:null,
                deleteEditResults!=null?deleteEditResults.deleteResults:null
        );

    }

    /**
     * Jackson does not convert anonymous JSON arrays, so this method adapted from
     * @see com.boundlessgeo.gsr.api.GSRModelReader
     * @param jsonString anonymous array of Features
     * @return FeatureArray object
     */
    private FeatureArray jsonStringToFeatureArray(String jsonString){
        JSON json = JSONSerializer.toJSON(jsonString);
            if (json instanceof JSONArray) {
                return LayerEditsEncoder.featureArrayFromJSON((JSONArray)json);
            }else{
                LOGGER.info("Submitted JSON is not an array, as expected.");
                throw new JSONException();
            }

    }

    /**
     * Jackson does not convert anonymous JSON arrays, so this method adapted from
     * @see com.boundlessgeo.gsr.api.GSRModelReader
     * @param jsonString anonymous array of Features
     * @return FeatureArray object
     */
    private ServiceEdits jsonStringToServiceEdits(String jsonString){
        JSON json = JSONSerializer.toJSON(jsonString);
        if (json instanceof JSONArray) {
            return LayerEditsEncoder.serviceEditsFromJSON((JSONArray)json);
        }else{
            LOGGER.info("Submitted JSON is not an array, as expected.");
            throw new JSONException();
        }

    }


    /**
     *
     * @param edits  Array of objects that specify the layer id and the edits to be applied, adds, updates, or deletes.  See https://developers.arcgis.com/rest/services-reference/apply-edits-feature-service-.htm for structure
     * @param workspaceName Workspace name
     * @param rollbackOnFailure edits succeed. If false, the server will apply the edits that succeed even if some of
     *      *                          the submitted edits fail. If true, the server will apply the edits only if all edits
     *      *                          succeed. The default value is true.
     * @param returnEditMoment Optional parameter specifying whether the response will report the time features were
     *      *                         updated. If returnEditMoment = true, the server will report the time in the response's
     *      *                         editMoment key. The default value is false.
     * @param honorSequenceOfEdits Optional parameter specifying whether to apply edits in the order submitted or by ascending layer id order
     *                             If true the edits will be applied in the order they are submitted.
     *                             If false(default) they will be applied in ascending layer-ID order.
     *
     * TODO: Unsupported parameters
     * f - only json supported (used by default), ignored
     * gdbVersion - GSR does not support versioned data, ignored.
     * trueCurveClient - GSR does not support true curve encoding, ignored.
     * useGlobalIds - GSR does not support global ids, ignored.
     * sessionID - GSR does not support session ids, ignored.
     * usePreviousEditMoment - GSR does not support merging of transactions with the editMoment, ignored.
     * returnServiceEditsOption - GSR does not support tracking of composite relationships, ignored.
     *
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    @PostMapping(path="/applyEdits", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public List<EditResults> applyEditsByService(
                                         @PathVariable String workspaceName,
                                         @RequestParam String edits,
                                         @RequestParam(name = "rollbackOnFailure", required = false, defaultValue = "false") boolean rollbackOnFailure,
                                         @RequestParam(name = "returnEditMoment", required = false, defaultValue = "false") boolean returnEditMoment,
                                         @RequestParam(name = "honorSequenceOfEdits", required = false, defaultValue = "false") boolean honorSequenceOfEdits)
            throws IOException, ServiceException {

        List<EditResults> editResults = new ArrayList<>();


        ServiceEdits serviceEdits = jsonStringToServiceEdits(edits);

        if(!honorSequenceOfEdits){  //sorts by id ascending if set to false
            serviceEdits.sortByID();
        }

        if(serviceEdits.layerEdits!=null&&serviceEdits.layerEdits.size()>0){
            for(LayerEdits layerEdits:serviceEdits.layerEdits){
                EditResults addEditResults=null;
                EditResults updateEditResults=null;
                EditResults deleteEditResults=null;
                if(layerEdits.getAdds()!=null&&layerEdits.getAdds().features!=null&&layerEdits.getAdds().features.size()>0){
                    addEditResults = addFeatures(layerEdits.getAdds(),workspaceName,layerEdits.getId(),returnEditMoment, rollbackOnFailure);

                }

                if(layerEdits.getUpdates()!=null&&layerEdits.getUpdates().features!=null&&layerEdits.getUpdates().features.size()>0){
                    updateEditResults = updateFeatures(layerEdits.getUpdates(),workspaceName,layerEdits.getId(), returnEditMoment, rollbackOnFailure);

                }

                if(layerEdits.getDeletes()!=null&&layerEdits.getDeletes().size()>0){
                    String objectIdString = layerEdits.getDeletes().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    deleteEditResults = deleteFeatures(workspaceName,layerEdits.getId(),objectIdString, returnEditMoment, rollbackOnFailure);

                }

                editResults.add(new EditResults(
                        addEditResults!=null?addEditResults.addResults:null,
                        updateEditResults!=null?updateEditResults.updateResults:null,
                        deleteEditResults!=null?deleteEditResults.deleteResults:null
                ));
            }

        }else{
            LOGGER.info("Submitted JSON is an empty ServiceEdits structure.");
        }

        return editResults;

    }
}