/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing GeoServer Layers.
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class LayerController extends CatalogController {
    private static final Logger LOGGER = Logging.getLogger(LayerController.class);

    @Autowired
    public LayerController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(value = "/layers", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<LayerInfo> getLayers() {
        List<LayerInfo> layers = catalog.getLayers();
        return wrapList(layers, LayerInfo.class);
    }
    
    @GetMapping(path = "/layers/{layerName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE})
    public RestWrapper<LayerInfo> getLayer( @PathVariable String layerName ){
        LayerInfo layer = catalog.getLayerByName(layerName);
        return wrapObject(layer,LayerInfo.class);
    }
}
