/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.jsonld.expressions.ExpressionsUtils;
import org.geoserver.jsonld.response.JSONLDGetFeatureResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.XCQL;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

/**
 * This {@link DispatcherCallback} implementation OGCAPI compliant that checks on operation
 * dispatched event if a json-ld path has been provided to cql_filter and evaluate it against the
 * {@link org.geoserver.jsonld.builders.JsonBuilder} tree to get the corresponding {@link Filter}
 */
public class JsonLdTemplateCallBackOGC extends AbstractDispatcherCallback {

    private Catalog catalog;

    private JsonLdConfiguration configuration;

    public JsonLdTemplateCallBackOGC(GeoServer gs, JsonLdConfiguration configuration) {
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        boolean isJsonLd =
                request.getKvp().get("f") != null
                                && request.getKvp().get("f").equals(JSONLDGetFeatureResponse.MIME)
                        ? true
                        : false;
        if (request.getHttpRequest().getServletPath().equals("/ogc") && isJsonLd) {
            try {
                String strFilter = (String) request.getKvp().get("FILTER");
                if (strFilter != null && strFilter.indexOf(".") != -1) {
                    FeatureTypeInfo typeInfo =
                            getFeatureType((String) operation.getParameters()[0]);
                    RootBuilder root = configuration.getTemplate(typeInfo);
                    if (root != null) {
                        JsonLdPathVisitor visitor =
                                new JsonLdPathVisitor(typeInfo.getFeatureType());
                        /* Todo find a better way to replace json-ld path with corresponding template attribute*/
                        // Get filter from string in order to make it accept the visitor
                        Filter f = (Filter) XCQL.toFilter(strFilter).accept(visitor, root);
                        // Taking back a string from Function cause
                        // OGC API get a string cql filter from query string
                        String newFilter = ECQL.toCQL(f).replaceAll("\"", "").replaceAll("/", ".");
                        newFilter = ExpressionsUtils.workXpathAttributeSyntax(newFilter);
                        for (int i = 0; i < operation.getParameters().length; i++) {
                            Object p = operation.getParameters()[i];
                            if (p != null
                                    && ((String.valueOf(p)).trim().equals(strFilter.trim()))) {
                                operation.getParameters()[i] = newFilter;
                                break;
                            }
                        }
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
}
