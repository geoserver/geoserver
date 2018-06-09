/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
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
    protected void qualifyRequest(
            WorkspaceInfo ws, PublishedInfo l, Service service, Request request) {
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

            String styles = (String) request.getRawKvp().get("STYLES");
            if (styles != null && !styles.trim().isEmpty()) {
                request.getRawKvp().put("STYLES", qualifyStyleNamesKVP(styles, ws));
            }

            String style = (String) request.getRawKvp().get("STYLE");
            if (style != null && !style.trim().isEmpty()) {
                request.getRawKvp().put("STYLE", qualifyStyleName(style, ws));
            }
        }
    }

    protected void qualifyRequest(
            WorkspaceInfo ws, PublishedInfo l, Operation operation, Request request) {
        GetCapabilitiesRequest gc = parameter(operation, GetCapabilitiesRequest.class);
        if (gc != null) {
            gc.setNamespace(ws.getName());
            return;
        }
    };

    String qualifyLayerNamesKVP(String layers, WorkspaceInfo ws) {
        List<String> list = KvpUtils.readFlat(layers);
        qualifyLayerNames(list, ws);

        return toCommaSeparatedList(list);
    }

    /**
     * Overriding the base class behavior as we want to avoid qualifying global layer group names
     */
    protected void qualifyLayerNames(List<String> names, WorkspaceInfo ws) {
        for (int i = 0; i < names.size(); i++) {
            String baseName = names.get(i);
            String qualified = qualifyName(baseName, ws);
            // only qualify if it's not a layer group (and prefer local layers to groups in case of
            // name clash), but also check for workspace specific layer groups
            if (catalog.getLayerByName(qualified) != null
                    || catalog.getLayerGroupByName(baseName) == null) {
                names.set(i, qualified);
            } else if (catalog.getLayerGroupByName(qualified) != null) {
                names.set(i, qualified);
            }
        }
    }

    String qualifyStyleNamesKVP(String styles, WorkspaceInfo ws) {
        List<String> list = KvpUtils.readFlat(styles);
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            name = qualifyStyleName(name, ws);
            list.set(i, name);
        }

        return toCommaSeparatedList(list);
    }

    private String qualifyStyleName(String name, WorkspaceInfo ws) {
        String qualified = qualifyName(name, ws);
        // does the qualified name exist?
        if (catalog.getStyleByName(qualified) != null) {
            return qualified;
        } else {
            // use the original name instead
            return name;
        }
    }

    private String toCommaSeparatedList(List<String> list) {
        return String.join(",", list);
    }
}
