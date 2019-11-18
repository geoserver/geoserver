/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * This {@link DispatcherCallback} implementation checks on operation dispatched event if a json-ld
 * path has been provided to cql_filter and evaluate it against the {@link
 * org.geoserver.jsonld.builders.JsonBuilder} tree to get the corresponding {@link Filter}
 */
public class JsonLdTemplateCallback implements DispatcherCallback {

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
        Name type = getFeatureTypeName(request);
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(type);

        if (typeInfo != null) {
            try {
                RootBuilder root =
                        JsonLdConfiguration.get()
                                .getTemplate(typeInfo, typeInfo.getName() + ".json");
                Map rawKvp = request.getRawKvp();
                if (rawKvp.get("CQL_FILTER") != null
                        && rawKvp.get("CQL_FILTER").toString().contains(".")
                        && root != null) {
                    GetFeatureRequest getFeature =
                            GetFeatureRequest.adapt(operation.getParameters()[0]);

                    if (getFeature != null && root != null) {
                        getFeature.getTypeName();
                        List<Query> queries = getFeature.getQueries();
                        if (queries != null && queries.size() > 0) {
                            JsonLdPathVisitor visitor = new JsonLdPathVisitor();
                            for (Query q : queries) {
                                if (q.getFilter() != null) {
                                    Filter newFilter = (Filter) q.getFilter().accept(visitor, root);
                                    q.setFilter(newFilter);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return operation;
    }

    private Name getFeatureTypeName(Request request) {
        Name typeName = null;
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
                }
                typeName = new NameImpl(ns.getURI(), last);
            }
        }
        return typeName;
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
