/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import java.io.OutputStream;
import org.geoserver.api.features.FeaturesResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.builders.TemplateBuilder;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.configuration.TemplateConfiguration;
import org.geoserver.jsonld.configuration.TemplateIdentifier;
import org.geoserver.jsonld.expressions.JsonLdCQLManager;
import org.geoserver.jsonld.response.GeoJsonTemplateGetFeatureResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.XCQL;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
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
        if (format != null
                && (format.equals(TemplateIdentifier.JSONLD.getOutputFormat())
                        || format.equals(TemplateIdentifier.GEOJSON.getOutputFormat()))) {
            return format;
        } else if (accept != null) {
            if (accept.contains(TemplateIdentifier.GEOJSON.getOutputFormat()))
                return TemplateIdentifier.GEOJSON.getOutputFormat();
            else if (accept.contains(TemplateIdentifier.JSONLD.getOutputFormat()))
                return TemplateIdentifier.JSONLD.getOutputFormat();
        }
        return null;
    }

    private void replaceJsonLdPathWithFilter(
            String strFilter, String outputFormat, Operation operation) throws Exception {
        if (strFilter != null && strFilter.indexOf(".") != -1) {
            FeatureTypeInfo typeInfo = getFeatureType((String) operation.getParameters()[0]);
            RootBuilder root = configuration.getTemplate(typeInfo, outputFormat);
            if (root != null) {
                JsonPathVisitor visitor = new JsonPathVisitor(typeInfo.getFeatureType());
                /* Todo find a better way to replace json-ld path with corresponding template attribute*/
                // Get filter from string in order to make it accept the visitor
                Filter f = (Filter) XCQL.toFilter(strFilter).accept(visitor, root);
                // Taking back a string from Function cause
                // OGC API get a string cql filter from query string
                String newFilter =
                        JsonLdCQLManager.removeQuotes(ECQL.toCQL(f)).replaceAll("/", ".");
                newFilter = JsonLdCQLManager.quoteXpathAttribute(newFilter);
                for (int i = 0; i < operation.getParameters().length; i++) {
                    Object p = operation.getParameters()[i];
                    if (p != null && ((String.valueOf(p)).trim().equals(strFilter.trim()))) {
                        operation.getParameters()[i] = newFilter;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        String format = getFormatSupportingTemplating(request);
        if (format != null && format.equals(TemplateIdentifier.GEOJSON.getOutputFormat())) {
            FeatureTypeInfo typeInfo = null;
            typeInfo = getFeatureType((String) operation.getParameters()[0]);
            if (typeInfo != null) {
                try {
                    RootBuilder root = configuration.getTemplate(typeInfo, format);
                    if (root != null) {
                        GeoJSONGetFeatureResponse featureResponse =
                                (GeoJSONGetFeatureResponse)
                                        GeoServerExtensions.bean("geoJSONGetFeatureResponse");
                        GeoJsonTemplateGetFeatureResponse templatingResp =
                                new GeoJsonTemplateGetFeatureResponse(
                                        gs, configuration, featureResponse) {
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

                        response = templatingResp;
                        request.getHttpResponse()
                                .setContentType(TemplateIdentifier.GEOJSON.getOutputFormat());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return response;
    }
}
