/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.WorkspaceQualifyingCallback;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;

public class WMSWorkspaceQualifier extends WorkspaceQualifyingCallback {

    public WMSWorkspaceQualifier(Catalog catalog) {
        super(catalog);
    }

    @Override
    protected void qualifyRequest(WorkspaceInfo ws, LayerInfo l, Service service, Request request) {
        if (WebMapService.class.isInstance(service.getService())) {
            String layers = (String) request.getRawKvp().get("LAYERS");
            if (layers != null) {
                request.getRawKvp().put("LAYERS", qualifyLayerNamesKVP(layers, ws));
            }

            layers = (String) request.getRawKvp().get("QUERY_LAYERS");
            if (layers != null) {
                request.getRawKvp().put("QUERY_LAYERS", qualifyLayerNamesKVP(layers, ws));
            }

            String layer = (String) request.getRawKvp().get("LAYER");
            if (layer != null) {
                request.getRawKvp().put("LAYER", qualifyName(layer, ws));
            }
        }
    }

    protected void qualifyRequest(WorkspaceInfo ws, LayerInfo l, Operation operation,
            Request request) {
        GetCapabilitiesRequest gc = parameter(operation, GetCapabilitiesRequest.class);
        if (gc != null) {
            gc.setNamespace(ws.getName());
            return;
        }
    };

    String qualifyLayerNamesKVP(String layers, WorkspaceInfo ws) {
        List<String> list = KvpUtils.readFlat(layers);
        qualifyNames(list, ws);

        StringBuffer sb = new StringBuffer();
        for (String s : list) {
            sb.append(s).append(",");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
    
    /**
     * Overriding the base class behavior as we want to avoid qualifying global layer group names
     */
    protected void qualifyNames(List<String> names, WorkspaceInfo ws) {
        for (int i = 0; i < names.size(); i++) {
            String baseName = names.get(i);
            String qualified = qualifyName(baseName, ws);
            // only qualify if it's not a layer group (and prefer local layers to groups in case of
            // name clash)
            if (catalog.getLayerByName(qualified) != null
                    || catalog.getLayerGroupByName(baseName) == null) {
                names.set(i, qualified);
            }
        }
    }

}
