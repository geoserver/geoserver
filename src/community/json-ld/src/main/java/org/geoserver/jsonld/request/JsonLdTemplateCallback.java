/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import java.util.logging.Logger;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;

public class JsonLdTemplateCallback implements DispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(JsonLdTemplateCallback.class);

    private Catalog catalog;

    private GeoServer gs;

    public JsonLdTemplateCallback(GeoServer gs) {
        this.gs = gs;
        this.catalog = gs.getCatalog();
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        String typename = (String) request.getRawKvp().get("TYPENAME");
        if (typename != null) {
            String first = typename;
            String last = null;

            int dots = first.indexOf(':');
            if (dots > -1) {
                last = first.substring(dots + 1);
                first = first.substring(0, dots);
            }
            WorkspaceInfo ws = catalog.getWorkspaceByName(first);
            if (last != null) {
                // hack up a qualified name
                NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                if (ns != null) {
                    // can have extra bits, like ws/layer/gwc/service
                    int slashInLayer = last.indexOf('/');
                    if (slashInLayer != -1) {
                        last = last.substring(0, slashInLayer);
                    }

                    FeatureTypeInfo typeInfo =
                            catalog.getFeatureTypeByName(new NameImpl(ns.getURI(), last));
                    JsonLdConfiguration configuration = JsonLdConfiguration.get();
                    // String[] steps = ((String) request.getKvp().get("JSONLD_FILTER")).split("/");
                    /*try {
                        AbstractJsonBuilder root =
                                configuration.getTemplate(typeInfo, typeInfo.getName() + ".json");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
                }
            }
        }

        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return response;
    }

    @Override
    public void finished(Request request) {}
}
