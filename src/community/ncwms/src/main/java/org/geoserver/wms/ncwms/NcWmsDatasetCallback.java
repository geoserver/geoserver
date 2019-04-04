/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geotools.util.Converters;

public class NcWmsDatasetCallback extends AbstractDispatcherCallback implements URLMangler {

    private Catalog catalog;

    public NcWmsDatasetCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Request init(Request request) {
        if (request.getRawKvp() == null) {
            return request;
        }

        String dataset = Converters.convert(request.getRawKvp().get("DATASET"), String.class);
        if (dataset != null) {
            WorkspaceInfo ws = catalog.getWorkspaceByName(dataset);
            if (ws != null) {
                LocalWorkspace.set(ws);
                return request;
            }

            LayerInfo layer = catalog.getLayerByName(dataset);
            if (layer != null) {
                LocalWorkspace.set(layer.getResource().getStore().getWorkspace());
                LocalPublished.set(layer);
                return request;
            }

            LayerGroupInfo group = catalog.getLayerGroupByName(dataset);
            if (group != null) {
                LocalWorkspace.set(group.getWorkspace());
                LocalPublished.set(group);
                return request;
            }
        }

        return request;
    }

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        Request request = Dispatcher.REQUEST.get();
        if (request != null
                && "GetCapabilities".equals(request.getRequest())
                && request.getRawKvp() != null) {
            String dataset = Converters.convert(request.getRawKvp().get("DATASET"), String.class);
            if (dataset != null) {
                kvp.put("DATASET", dataset);
            }
        }
    }
}
