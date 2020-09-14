/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.jsonld.JsonLdRootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateConfiguration;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.response.GeoJsonTemplateGetFeatureResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * This {@link DispatcherCallback} implementation checks on operation dispatched event if a json-ld
 * path has been provided to cql_filter and evaluate it against the {@link TemplateBuilder} tree to
 * get the corresponding {@link Filter}
 */
public class JsonTemplateCallback extends AbstractDispatcherCallback {

    private Catalog catalog;

    private GeoServer gs;

    private TemplateConfiguration configuration;

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

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
                                .equals(TemplateIdentifier.JSON.getOutputFormat()))) {
            try {
                GetFeatureRequest getFeature =
                        GetFeatureRequest.adapt(operation.getParameters()[0]);
                List<Query> queries = getFeature.getQueries();
                if (getFeature != null && queries != null && queries.size() > 0) {
                    handleTemplateFiltersAndValidation(
                            queries, request.getOutputFormat(), isValidation(request));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.operationDispatched(request, operation);
    }

    private boolean isValidation(Request request) {
        Map rawKvp = request.getRawKvp();
        if (rawKvp != null) {
            Object validation = request.getRawKvp().get("validation");
            return validation != null ? Boolean.valueOf(validation.toString()) : false;
        }
        return false;
    }

    // iterate over queries to eventually handle a templates query paths
    private void handleTemplateFiltersAndValidation(
            List<Query> queries, String outputFormat, boolean validation)
            throws ExecutionException {
        for (Query q : queries) {
            List<FeatureTypeInfo> featureTypeInfos = getFeatureTypeInfoFromQuery(q);
            List<RootBuilder> rootBuilders =
                    getRootBuildersFromFeatureTypeInfo(featureTypeInfos, outputFormat);
            if (rootBuilders.size() > 0) {
                for (int i = 0; i < featureTypeInfos.size(); i++) {
                    FeatureTypeInfo fti = featureTypeInfos.get(i);
                    RootBuilder root = rootBuilders.get(i);
                    if (validation && root instanceof JsonLdRootBuilder) {
                        ((JsonLdRootBuilder) root).setSemanticValidation(validation);
                    }
                    replaceTemplatePath(q, fti, root);
                }
            }
        }
    }

    // get the FeatureTypeInfo from the query
    private List<FeatureTypeInfo> getFeatureTypeInfoFromQuery(Query q) {
        List<FeatureTypeInfo> typeInfos = new ArrayList<>();
        for (QName typeName : q.getTypeNames()) {
            typeInfos.add(
                    catalog.getFeatureTypeByName(
                            new NameImpl(typeName.getPrefix(), typeName.getLocalPart())));
        }
        return typeInfos;
    }

    private List<RootBuilder> getRootBuildersFromFeatureTypeInfo(
            List<FeatureTypeInfo> typeInfos, String outputFormat) throws ExecutionException {
        List<RootBuilder> rootBuilders = new ArrayList<>();
        int nullRootIndex = 0;
        for (int i = 0; i < typeInfos.size(); i++) {
            FeatureTypeInfo fti = typeInfos.get(i);
            RootBuilder root = ensureTemplatesExist(fti, outputFormat);
            if (root == null) nullRootIndex = i;
            else rootBuilders.add(root);
        }
        int rootsSize = rootBuilders.size();
        if (rootsSize > 0 && rootsSize != typeInfos.size()) {
            // we are missing a template throwing exception
            throw new RuntimeException(
                    "No template found for feature type "
                            + typeInfos.get(nullRootIndex).getName()
                            + " for output format "
                            + outputFormat);
        }
        return rootBuilders;
    }

    // invokes the path visitor to map the  template path if present
    // to the pointed template attribute and set the new filter to the query
    private void replaceTemplatePath(Query q, FeatureTypeInfo fti, RootBuilder root) {
        try {

            JsonPathVisitor visitor = new JsonPathVisitor(fti.getFeatureType());
            if (q.getFilter() != null) {
                Filter old = q.getFilter();
                String cql = ECQL.toCQL(old);
                if (cql.contains("features/") || cql.contains("features.")) {
                    Filter newFilter = (Filter) old.accept(visitor, root);
                    List<Filter> templateFilters = new ArrayList<>();
                    templateFilters.addAll(visitor.getFilters());
                    if (templateFilters != null && templateFilters.size() > 0) {
                        templateFilters.add(newFilter);
                        newFilter = ff.and(templateFilters);
                    }
                    q.setFilter(newFilter);
                    if (newFilter.equals(old)) {
                        throw new RuntimeException(
                                "Failed to resolve filter "
                                        + cql
                                        + " against the template. "
                                        + "Check the path specified in the filter.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        if (response instanceof GeoJSONGetFeatureResponse) {
            GetFeatureRequest getFeature = GetFeatureRequest.adapt(operation.getParameters()[0]);
            List<Query> queries = getFeature.getQueries();
            for (Query q : queries) {
                List<FeatureTypeInfo> typeInfos = getFeatureTypeInfoFromQuery(q);
                Response wrapped = wrapGeoJSONResponse(typeInfos, request.getOutputFormat());
                if (wrapped != null) response = wrapped;
            }
        }
        return super.responseDispatched(request, operation, result, response);
    }

    private Response wrapGeoJSONResponse(List<FeatureTypeInfo> typeInfos, String outputFormat) {
        Response response = null;
        try {

            List<RootBuilder> rootBuilders =
                    getRootBuildersFromFeatureTypeInfo(typeInfos, outputFormat);
            if (rootBuilders.size() > 0) {
                response =
                        new GeoJsonTemplateGetFeatureResponse(
                                gs, configuration, TemplateIdentifier.JSON);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    // get the root builder and eventually throws exception if it is
    // null and json-ld output is requested
    private RootBuilder ensureTemplatesExist(FeatureTypeInfo typeInfo, String outputFormat)
            throws ExecutionException {

        RootBuilder root = configuration.getTemplate(typeInfo, outputFormat);
        if (outputFormat.equals(TemplateIdentifier.JSONLD.getOutputFormat()) && root == null) {
            throw new RuntimeException(
                    "No template found for feature type "
                            + typeInfo.getName()
                            + " for output format "
                            + outputFormat);
        }
        return root;
    }
}
