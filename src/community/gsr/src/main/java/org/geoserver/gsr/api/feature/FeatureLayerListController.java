/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.feature;

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

/** Controller for the Feature Service layers list endpoint */
@RestController
@RequestMapping(
        path = "/gsr/services/{workspaceName}/FeatureServer",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureLayerListController extends AbstractGSRController {

    @Autowired
    public FeatureLayerListController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/layers", name = "FeatureServerGetLayers")
    @HTMLResponseBody(templateName = "featurelayers.ftl", fileName = "featurelayers.html")
    public LayersAndTables getLayers(@PathVariable String workspaceName) {
        LayersAndTables layers = LayerDAO.find(catalog, workspaceName);
        layers.getPath()
                .addAll(
                        Arrays.asList(
                                new Link(workspaceName, workspaceName),
                                new Link(workspaceName + "/" + "FeatureServer", "FeatureServer")));
        layers.getInterfaces()
                .add(
                        new Link(
                                workspaceName + "/" + "FeatureServer/layers?f=json&pretty=true",
                                "REST"));
        return layers;
    }
}
