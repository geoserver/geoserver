/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.catalog;

import static org.geoserver.gsr.GSRConfig.CURRENT_VERSION;
import static org.geoserver.gsr.GSRConfig.PRODUCT_NAME;
import static org.geoserver.gsr.GSRConfig.SPEC_VERSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.GSRServiceInfo;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.model.AbstractGSRModel.Link;
import org.geoserver.gsr.model.service.AbstractService;
import org.geoserver.gsr.model.service.CatalogService;
import org.geoserver.gsr.model.service.FeatureService;
import org.geoserver.gsr.model.service.GeometryService;
import org.geoserver.gsr.model.service.MapService;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the root Catalog service endpoint. */
@APIService(
        service = "GSR",
        version = "10.51",
        landingPage = "gsr/services",
        core = true,
        serviceClass = GSRServiceInfo.class)
@RestController
@RequestMapping(path = "/gsr/services", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogServiceController extends AbstractGSRController {

    @Autowired
    public CatalogServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(
            path = {""},
            name = "GetServices")
    @HTMLResponseBody(templateName = "catalog.ftl", fileName = "catalog.html")
    public CatalogService catalogGet() {
        List<AbstractService> services = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        for (WorkspaceInfo ws : catalog.getWorkspaces()) {
            folders.add(ws.getName());
            fillServices(services, ws);
        }
        services.add(new GeometryService("Geometry"));
        CatalogService catalog =
                new CatalogService(
                        "/", SPEC_VERSION, PRODUCT_NAME, CURRENT_VERSION, folders, services);
        catalog.getInterfaces().add(new Link("?f=json&pretty=true", "REST"));
        return catalog;
    }

    @GetMapping(
            path = {"/{folder:.*}"},
            name = "GetServices")
    @HTMLResponseBody(templateName = "catalog.ftl", fileName = "catalog.html")
    public CatalogService catalogGet(@PathVariable(required = true) String folder) {
        List<AbstractService> services = new ArrayList<>();
        WorkspaceInfo ws = catalog.getWorkspaceByName(folder);
        if (ws == null) {
            throw new NoSuchElementException(
                    "Workspace name " + folder + " does not correspond to any workspace.");
        }
        fillServices(services, ws);
        CatalogService catalog =
                new CatalogService(
                        folder,
                        SPEC_VERSION,
                        PRODUCT_NAME,
                        CURRENT_VERSION,
                        Collections.emptyList(),
                        services);
        catalog.getPath().add(new Link(folder, folder));
        catalog.getInterfaces().add(new Link(folder + "?f=json&pretty=true", "REST"));
        return catalog;
    }

    private void fillServices(List<AbstractService> services, WorkspaceInfo ws) {
        MapService ms = new MapService(ws.getName());
        FeatureService fs = new FeatureService(ws.getName());
        services.add(ms);
        services.add(fs);
    }
}
