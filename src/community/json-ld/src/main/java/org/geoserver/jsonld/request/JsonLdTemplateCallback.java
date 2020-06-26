/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.jsonld.response.JSONLDGetFeatureResponse;
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

    private JsonLdConfiguration configuration;

    public JsonLdTemplateCallback(GeoServer gs, JsonLdConfiguration configuration) {
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if ("WFS".equalsIgnoreCase(request.getService())
                && request.getOutputFormat() != null
                && request.getOutputFormat().equals(JSONLDGetFeatureResponse.MIME)) {
            FeatureTypeInfo typeInfo = null;
            GetFeatureRequest getFeature = null;
            getFeature = GetFeatureRequest.adapt(operation.getParameters()[0]);
            List<Query> queries = getFeature.getQueries();
            if (getFeature != null && queries != null && queries.size() > 0) {
                for (int i = 0; i < queries.size(); i++) {
                    Query q = queries.get(i);
                    QName type = q.getTypeNames().get(0);

                    typeInfo =
                            catalog.getFeatureTypeByName(
                                    new NameImpl(type.getPrefix(), type.getLocalPart()));
                    if (typeInfo != null) {
                        try {
                            RootBuilder root = configuration.getTemplate(typeInfo);
                            JsonLdPathVisitor visitor =
                                    new JsonLdPathVisitor(typeInfo.getFeatureType());
                            if (q.getFilter() != null) {
                                Filter newFilter = (Filter) q.getFilter().accept(visitor, root);
                                q.setFilter(newFilter);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return super.operationDispatched(request, operation);
    }
}
