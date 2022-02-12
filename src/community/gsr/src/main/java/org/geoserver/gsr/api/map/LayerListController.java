/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.map;

import java.util.Arrays;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.model.AbstractGSRModel.Link;
import org.geoserver.gsr.model.map.LayersAndTables;
import org.geoserver.gsr.translate.map.LayerDAO;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the Map Service layers list endpoint */
@RestController
@RequestMapping(
        path = "/gsr/services/{workspaceName}/MapServer",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class LayerListController extends AbstractGSRController {

    @Autowired
    public LayerListController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/layers", name = "MapServerGetLayers")
    @HTMLResponseBody(templateName = "maplayers.ftl", fileName = "maplayers.html")
    public LayersAndTables getLayers(@PathVariable String workspaceName) {
        LayersAndTables layers = LayerDAO.find(catalog, workspaceName);
        layers.getPath()
                .addAll(
                        Arrays.asList(
                                new Link(workspaceName, workspaceName),
                                new Link(workspaceName + "/" + "MapServer", "MapServer")));
        layers.getInterfaces()
                .add(new Link(workspaceName + "/" + "MapServer/layers?f=json&pretty=true", "REST"));
        return layers;
    }
}
