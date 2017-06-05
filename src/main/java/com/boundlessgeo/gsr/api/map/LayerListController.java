package com.boundlessgeo.gsr.api.map;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.core.map.LayersAndTables;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/MapServer", produces = MediaType.APPLICATION_JSON_VALUE)
public class LayerListController extends AbstractGSRController {

    @Autowired
    public LayerListController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/layers")
    public LayersAndTables layersGet(@PathVariable String workspaceName) {
        return LayersAndTables.find(catalog, workspaceName);
    }
}
