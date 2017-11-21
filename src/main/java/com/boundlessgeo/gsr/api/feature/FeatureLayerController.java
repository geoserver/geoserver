package com.boundlessgeo.gsr.api.feature;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.core.feature.FeatureLayer;
import com.boundlessgeo.gsr.core.map.LayerOrTable;
import com.boundlessgeo.gsr.core.map.LayersAndTables;

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
            entry = LayersAndTables.find(catalog, workspaceName, layerId);
        } catch (IOException e) {
            throw new NoSuchElementException("Unavailable table or layer in workspace \"" + workspaceName + "\" for id " + layerId + ":" + e);
        }
        if (entry == null) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspaceName + "\" for id " + layerId);
        }

        return new FeatureLayer(entry);
    }
}
