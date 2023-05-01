/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.model.map.LayerLegend;
import org.geoserver.gsr.model.map.LayerNameComparator;
import org.geoserver.gsr.model.map.Legends;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the Map Service legend endpoint */
@RestController
@RequestMapping(
        path = "/gsr/services/{workspaceName}/MapServer",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class LegendController extends AbstractGSRController {

    @Autowired
    public LegendController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/legend", name = "MapServerGetLegend")
    public Legends getLegend(@PathVariable String workspaceName) throws IOException {
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("No workspace known by name: " + workspaceName);
        }

        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.getType() == PublishedType.VECTOR
                    && l.getResource().getStore().getWorkspace().getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);
        List<LayerLegend> legends = new ArrayList<>();
        for (int i = 0; i < layersInWorkspace.size(); i++) {
            legends.add(new LayerLegend(layersInWorkspace.get(i), i));
        }
        return new Legends(legends);
    }
}
