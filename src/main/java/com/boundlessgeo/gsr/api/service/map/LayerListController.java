package com.boundlessgeo.gsr.api.service.map;

import com.boundlessgeo.gsr.model.map.LayersAndTables;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LayerListController extends AbstractMapServiceController {

    @Autowired
    public LayerListController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/layers")
    public LayersAndTables layersGet(@PathVariable String workspaceName) {
        return LayersAndTables.find(catalog, workspaceName);
    }
}
