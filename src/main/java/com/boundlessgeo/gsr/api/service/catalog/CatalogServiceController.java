package com.boundlessgeo.gsr.api.service.catalog;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.service.*;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(path = "/gsr/services", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogServiceController extends AbstractGSRController {

    private String formatValue;

    private final String productName = "OpenGeo Suite";

    private final String specVersion = "1.0";

    private final double currentVersion = 10.1;

    @Autowired
    public CatalogServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = {"", "/{folder}"})
    public CatalogService catalogGet(@PathVariable(required = false) String folder) {
        List<AbstractService> services = new ArrayList<AbstractService>();
        for (WorkspaceInfo ws : catalog.getWorkspaces()) {
            MapService ms = new MapService(ws.getName());
            FeatureService fs = new FeatureService(ws.getName());
            services.add(ms);
            services.add(fs);
        }
        services.add(new GeometryService("Geometry"));
        return new CatalogService("services", specVersion, productName, currentVersion, Collections.<String>emptyList(), services);
    }
}
