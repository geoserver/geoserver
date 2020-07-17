/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.request;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.api.features.FeaturesResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.XCQL;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.expressions.JsonLdCQLManager;
import org.geoserver.wfstemplating.response.GeoJsonTemplateGetFeatureResponse;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.springframework.http.HttpHeaders;

/**
 * This {@link DispatcherCallback} implementation OGCAPI compliant that checks on operation
 * dispatched event if a json-ld path has been provided to cql_filter and evaluate it against the
 * {@link TemplateBuilder} tree to get the corresponding {@link Filter}
 */
public class JsonTemplateCallBackOGC extends AbstractDispatcherCallback {

    private Catalog catalog;

    private TemplateConfiguration configuration;

    private GeoServer gs;

    public JsonTemplateCallBackOGC(GeoServer gs, TemplateConfiguration configuration) {
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
        this.gs = gs;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        String outputFormat = getFormatSupportingTemplating(request);
        if ("FEATURES".equalsIgnoreCase(request.getService()) && outputFormat != null) {
            try {
                String filterLang = (String) request.getKvp().get("FILTER-LANG");
                if (filterLang != null && filterLang.equalsIgnoreCase("CQL-TEXT")) {
                    String filter = (String) request.getKvp().get("FILTER");
                    replaceJsonLdPathWithFilter(filter, outputFormat, operation);
                }
                String envParam =
                        request.getRawKvp().get("ENV") != null
                                ? request.getRawKvp().get("ENV").toString()
                                : null;

                if (envParam != null) {
                    String[] arEnvParams = envParam.split(";");
                    for (String e : arEnvParams) {
                        String[] singleParam = e.split(":");
                        EnvFunction.setLocalValue(singleParam[0], singleParam[1]);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.operationDispatched(request, operation);
    }

    private FeatureTypeInfo getFeatureType(String collectionId) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(collectionId);
        if (featureType == null) {
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        return featureType;
    }

    private String getFormatSupportingTemplating(Request request) {
        String accept = request.getHttpRequest().getHeader(HttpHeaders.ACCEPT);
        String format = request.getKvp() != null ? (String) request.getKvp().get("f") : null;
        if (format != null && isFormatSupported(format)) {
            return format;
        } else if (accept != null) {
            if (accept.contains(TemplateIdentifier.JSON.getOutputFormat()))
                return TemplateIdentifier.JSON.getOutputFormat();
            else if (accept.contains(TemplateIdentifier.JSONLD.getOutputFormat()))
                return TemplateIdentifier.JSONLD.getOutputFormat();
            else if (accept.contains(TemplateIdentifier.GEOJSON.getOutputFormat()))
                return TemplateIdentifier.GEOJSON.getOutputFormat();
        }
        return null;
    }

    private boolean isFormatSupported(String format) {
        return format.equals(TemplateIdentifier.JSONLD.getOutputFormat())
                || format.equals(TemplateIdentifier.JSON.getOutputFormat())
                || format.equals(TemplateIdentifier.GEOJSON.getOutputFormat());
    }

    private void replaceJsonLdPathWithFilter(
            String strFilter, String outputFormat, Operation operation) throws Exception {
        FeatureTypeInfo typeInfo = getFeatureType((String) operation.getParameters()[0]);
        RootBuilder root = configuration.getTemplate(typeInfo, outputFormat);
        if (strFilter != null && strFilter.indexOf(".") != -1) {
            if (root != null) {
                replaceFilter(strFilter, root, typeInfo, operation);
            }
        }
    }

    private void replaceFilter(
            String strFilter, RootBuilder root, FeatureTypeInfo typeInfo, Operation operation)
            throws IOException, CQLException {
        JsonPathVisitor visitor = new JsonPathVisitor(typeInfo.getFeatureType());
        /* Todo find a better way to replace json-ld path with corresponding template attribute*/
        // Get filter from string in order to make it accept the visitor
        Filter f = (Filter) XCQL.toFilter(strFilter).accept(visitor, root);
        List<Filter> templateFilters = new ArrayList<>();
        templateFilters.addAll(visitor.getFilters());
        if (templateFilters != null && templateFilters.size() > 0) {
            templateFilters.add(f);
            f = JsonTemplateCallback.ff.and(templateFilters);
        }
        // Taking back a string from Function cause
        // OGC API get a string cql filter from query string
        String newFilter = JsonLdCQLManager.removeQuotes(ECQL.toCQL(f)).replaceAll("/", ".");
        newFilter = JsonLdCQLManager.quoteXpathAttribute(newFilter);
        for (int i = 0; i < operation.getParameters().length; i++) {
            Object p = operation.getParameters()[i];
            if (p != null && ((String.valueOf(p)).trim().equals(strFilter.trim()))) {
                operation.getParameters()[i] = newFilter;
                break;
            }
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        String format = getFormatSupportingTemplating(request);
        boolean isJson = format != null && format.equals(TemplateIdentifier.JSON.getOutputFormat());
        boolean isGeoJson =
                format != null && format.equals(TemplateIdentifier.GEOJSON.getOutputFormat());
        if (isJson || isGeoJson) {
            FeatureTypeInfo typeInfo = getFeatureType((String) operation.getParameters()[0]);
            if (typeInfo != null) {
                try {
                    RootBuilder root = configuration.getTemplate(typeInfo, format);
                    if (root != null) {
                        response =
                                wrapResponse(
                                        operation,
                                        result,
                                        isGeoJson
                                                ? TemplateIdentifier.GEOJSON
                                                : TemplateIdentifier.JSON);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return response;
    }

    private Response wrapResponse(
            Operation operation, Object result, TemplateIdentifier identifier) {

        GeoJsonTemplateGetFeatureResponse templatingResp =
                new GeoJsonTemplateGetFeatureResponse(gs, configuration, identifier) {
                    @Override
                    protected void write(
                            FeatureCollectionResponse featureCollection,
                            OutputStream output,
                            Operation getFeature)
                            throws ServiceException {
                        FeaturesResponse fr = (FeaturesResponse) result;
                        super.write(fr.getResponse(), output, operation);
                    }
                };
        return templatingResp;
    }
}
