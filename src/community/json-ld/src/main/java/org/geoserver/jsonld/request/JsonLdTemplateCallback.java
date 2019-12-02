/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.feature.NameImpl;
import org.opengis.filter.Filter;

/**
 * This {@link DispatcherCallback} implementation checks on operation dispatched event if a json-ld
 * path has been provided to cql_filter and evaluate it against the {@link
 * org.geoserver.jsonld.builders.JsonBuilder} tree to get the corresponding {@link Filter}
 */
public class JsonLdTemplateCallback extends AbstractDispatcherCallback {

    private Catalog catalog;

    private GeoServer gs;

    private JsonLdConfiguration configuration;

    public JsonLdTemplateCallback(GeoServer gs, JsonLdConfiguration configuration) {
        this.gs = gs;
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        FeatureTypeInfo typeInfo = null;
        GetFeatureRequest getFeature = GetFeatureRequest.adapt(operation.getParameters()[0]);

        if (getFeature != null) {
            Map rawKvp = request.getRawKvp();
            boolean jump =
                    request.getHttpRequest().getMethod().equalsIgnoreCase("GET")
                            && (rawKvp.get("CQL_FILTER") == null
                                    || !rawKvp.get("CQL_FILTER").toString().contains("."));
            if (!jump) {
                for (int i = 0; i < getFeature.getQueries().size(); i++) {
                    Query q = getFeature.getQueries().get(i);
                    QName type = q.getTypeNames().get(0);

                    typeInfo =
                            catalog.getFeatureTypeByName(
                                    new NameImpl(type.getPrefix(), type.getLocalPart()));
                    if (typeInfo != null) {
                        try {
                            RootBuilder root =
                                    configuration.getTemplate(
                                            typeInfo, typeInfo.getName() + ".json");
                            JsonLdPathVisitor visitor = new JsonLdPathVisitor();
                            if (q.getFilter() != null) {
                                Filter newFilter = (Filter) q.getFilter().accept(visitor, root);
                                q.setFilter(newFilter);
                                getFeature.getQueries().set(i, q);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return operation;
    }
}
