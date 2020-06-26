/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.request;

import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.response.GeoJsonTemplateGetFeatureResponse;
import org.geotools.feature.NameImpl;
import org.opengis.filter.Filter;

/**
 * This {@link DispatcherCallback} implementation checks on operation dispatched event if a json-ld
 * path has been provided to cql_filter and evaluate it against the {@link TemplateBuilder} tree to
 * get the corresponding {@link Filter}
 */
public class JsonTemplateCallback extends AbstractDispatcherCallback {

    private Catalog catalog;

    private GeoServer gs;

    private TemplateConfiguration configuration;

    public JsonTemplateCallback(GeoServer gs, TemplateConfiguration configuration) {
        this.gs = gs;
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if ("WFS".equalsIgnoreCase(request.getService())
                && request.getOutputFormat() != null
                && (request.getOutputFormat().equals(TemplateIdentifier.JSONLD.getOutputFormat())
                        || request.getOutputFormat()
                                .equals(TemplateIdentifier.GEOJSON.getOutputFormat()))) {
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
                            RootBuilder root =
                                    configuration.getTemplate(typeInfo, request.getOutputFormat());
                            if (root != null) {
                                JsonPathVisitor visitor =
                                        new JsonPathVisitor(typeInfo.getFeatureType());
                                if (q.getFilter() != null) {
                                    Filter newFilter = (Filter) q.getFilter().accept(visitor, root);
                                    q.setFilter(newFilter);
                                }
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

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        if (response instanceof GeoJSONGetFeatureResponse) {
            FeatureTypeInfo typeInfo = null;
            GetFeatureRequest getFeature = null;
            getFeature = GetFeatureRequest.adapt(operation.getParameters()[0]);
            QName type = getFeature.getQueries().get(0).getTypeNames().get(0);
            typeInfo =
                    catalog.getFeatureTypeByName(
                            new NameImpl(type.getPrefix(), type.getLocalPart()));
            if (typeInfo != null) {
                try {
                    RootBuilder root =
                            configuration.getTemplate(typeInfo, request.getOutputFormat());
                    if (root != null) {
                        response =
                                new GeoJsonTemplateGetFeatureResponse(
                                        gs, configuration, (GeoJSONGetFeatureResponse) response);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.responseDispatched(request, operation, result, response);
    }
}
