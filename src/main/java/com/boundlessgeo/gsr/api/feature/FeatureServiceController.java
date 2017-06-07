package com.boundlessgeo.gsr.api.feature;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.core.feature.FeatureServiceRoot;
import com.boundlessgeo.gsr.core.map.LayerNameComparator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
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
import java.util.NoSuchElementException;

/**
 * Controller for the root Feature Service endpoint
 */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/FeatureServer", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureServiceController extends AbstractGSRController {

    @Autowired
    public FeatureServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping
    public FeatureServiceRoot featureServiceGet(@PathVariable String workspaceName) {

        WorkspaceInfo workspace = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("Workspace name " + workspaceName + " does not correspond to any workspace.");
        }
        WFSInfo service = geoServer.getService(workspace, WFSInfo.class);
        if (service == null) {
            service = geoServer.getService(WFSInfo.class);
        }
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : geoServer.getCatalog().getLayers()) {
            if (l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace().equals(workspace)) {
                layersInWorkspace.add(l);
            }
        }
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);
        return new FeatureServiceRoot(service, Collections.unmodifiableList(layersInWorkspace));
    }
}
