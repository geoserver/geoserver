/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.api.catalog;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.service.*;
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

import static com.boundlessgeo.gsr.GSRConfig.CURRENT_VERSION;
import static com.boundlessgeo.gsr.GSRConfig.PRODUCT_NAME;
import static com.boundlessgeo.gsr.GSRConfig.SPEC_VERSION;

/**
 * Controller for the root Catalog service endpoint.
 */
@RestController
@RequestMapping(path = "/gsr/services", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogServiceController extends AbstractGSRController {

    @Autowired
    public CatalogServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = {"", "/{folder}"})
    public CatalogService catalogGet(@PathVariable(required = false) String folder) {
        List<AbstractService> services = new ArrayList<>();
        for (WorkspaceInfo ws : catalog.getWorkspaces()) {
            MapService ms = new MapService(ws.getName());
            FeatureService fs = new FeatureService(ws.getName());
            services.add(ms);
            services.add(fs);
        }
        services.add(new GeometryService("Geometry"));
        return new CatalogService("services", SPEC_VERSION, PRODUCT_NAME, CURRENT_VERSION, Collections.emptyList(), services);
    }
}
