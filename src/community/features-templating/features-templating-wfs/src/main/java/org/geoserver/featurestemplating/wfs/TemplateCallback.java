/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.wfs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateConfiguration;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.request.TemplatePathVisitor;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * This {@link DispatcherCallback} implementation checks on operation dispatched event if a json-ld
 * path has been provided to cql_filter and evaluate it against the {@link TemplateBuilder} tree to
 * get the corresponding {@link Filter}
 */
public class TemplateCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(TemplateCallback.class);

    private Catalog catalog;

    private GeoServer gs;

    private TemplateConfiguration configuration;

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    public TemplateCallback(GeoServer gs, TemplateConfiguration configuration) {
        this.gs = gs;
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if (request.getService().toUpperCase().contains("WFS")) {
            try {
                GetFeatureRequest getFeature =
                        GetFeatureRequest.adapt(operation.getParameters()[0]);
                if (getFeature != null) {
                    List<Query> queries = getFeature.getQueries();
                    if (queries != null && queries.size() > 0) {
                        handleTemplateFilters(queries, request.getOutputFormat());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.operationDispatched(request, operation);
    }

    // iterate over queries to eventually handle a templates query paths
    private void handleTemplateFilters(List<Query> queries, String outputFormat)
            throws ExecutionException {
        for (Query q : queries) {
            List<FeatureTypeInfo> featureTypeInfos = getFeatureTypeInfoFromQuery(q);
            List<RootBuilder> rootBuilders =
                    getRootBuildersFromFeatureTypeInfo(featureTypeInfos, outputFormat);
            if (rootBuilders.size() > 0) {
                for (int i = 0; i < featureTypeInfos.size(); i++) {
                    FeatureTypeInfo fti = featureTypeInfos.get(i);
                    RootBuilder root = rootBuilders.get(i);
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

            TemplatePathVisitor visitor = new TemplatePathVisitor(fti.getFeatureType());
            if (q.getFilter() != null) {
                Filter old = q.getFilter();
                String cql = ECQL.toCQL(old);
                Filter newFilter = (Filter) old.accept(visitor, root);
                List<Filter> templateFilters = new ArrayList<>();
                templateFilters.addAll(visitor.getFilters());
                if (templateFilters != null && templateFilters.size() > 0) {
                    templateFilters.add(newFilter);
                    newFilter = ff.and(templateFilters);
                }
                q.setFilter(newFilter);
                if (newFilter.equals(old)) {
                    LOGGER.warning(
                            "Failed to resolve filter "
                                    + cql
                                    + " against the template. "
                                    + "If the property name was intended to be a template path, "
                                    + "check that the path specified in the cql filter is correct.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        Object[] params = operation.getParameters();
        if (params.length > 0) {
            GetFeatureRequest getFeature = GetFeatureRequest.adapt(params[0]);
            if (getFeature != null) {
                List<Query> queries = getFeature.getQueries();
                for (Query q : queries) {
                    List<FeatureTypeInfo> typeInfos = getFeatureTypeInfoFromQuery(q);
                    Response templateResponse =
                            getTemplateResponse(typeInfos, request.getOutputFormat());
                    if (templateResponse != null) response = templateResponse;
                }
            }
        }
        return super.responseDispatched(request, operation, result, response);
    }

    private Response getTemplateResponse(List<FeatureTypeInfo> typeInfos, String outputFormat) {
        Response response = null;
        try {

            List<RootBuilder> rootBuilders =
                    getRootBuildersFromFeatureTypeInfo(typeInfos, outputFormat);
            if (rootBuilders.size() > 0) {
                TemplateIdentifier templateIdentifier =
                        TemplateIdentifier.getTemplateIdentifierFromOutputFormat(outputFormat);
                switch (templateIdentifier) {
                    case JSON:
                    case GEOJSON:
                        response =
                                new GeoJSONTemplateGetFeatureResponse(
                                        gs, configuration, templateIdentifier);
                        break;
                    case GML32:
                    case GML31:
                    case GML2:
                        response = new GMLTemplateResponse(gs, configuration, templateIdentifier);
                        break;
                }
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
        TemplateIdentifier identifier =
                TemplateIdentifier.getTemplateIdentifierFromOutputFormat(outputFormat);
        RootBuilder rootBuilder = null;
        if (identifier != null) {
            rootBuilder = configuration.getTemplate(typeInfo, identifier.getOutputFormat());
            if (outputFormat.equals(TemplateIdentifier.JSONLD.getOutputFormat())
                    && rootBuilder == null) {
                throw new RuntimeException(
                        "No template found for feature type "
                                + typeInfo.getName()
                                + " for output format "
                                + outputFormat);
            }
        }
        return rootBuilder;
    }
}
