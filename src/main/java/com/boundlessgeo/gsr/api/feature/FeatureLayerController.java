package com.boundlessgeo.gsr.api.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.boundlessgeo.gsr.model.feature.EditResults;
import com.boundlessgeo.gsr.model.feature.Feature;
import com.boundlessgeo.gsr.model.feature.FeatureArray;
import com.boundlessgeo.gsr.translate.feature.FeatureDAO;
import com.boundlessgeo.gsr.translate.map.LayerDAO;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.feature.FeatureLayer;
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
}
