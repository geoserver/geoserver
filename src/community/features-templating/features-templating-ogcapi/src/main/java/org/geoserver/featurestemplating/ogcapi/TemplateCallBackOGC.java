/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ogcapi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geoserver.featurestemplating.ows.TemplateCallback;
import org.geoserver.featurestemplating.ows.wfs.BaseTemplateGetFeatureResponse;
import org.geoserver.featurestemplating.ows.wfs.GMLTemplateResponse;
import org.geoserver.featurestemplating.ows.wfs.HTMLTemplateResponse;
import org.geoserver.featurestemplating.request.TemplatePathVisitor;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.v1.features.FeatureService;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.XCQL;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpHeaders;

/**
 * This {@link DispatcherCallback} implementation OGCAPI compliant that checks on operation dispatched event if a
 * json-ld path has been provided to cql_filter and evaluate it against the {@link TemplateBuilder} tree to get the
 * corresponding {@link Filter}
 */
public class TemplateCallBackOGC extends AbstractDispatcherCallback {

    static final FormatOptionsKvpParser PARSER = new FormatOptionsKvpParser("env");

    private static final Logger LOGGER = Logging.getLogger(TemplateCallback.class);

    private Catalog catalog;

    private TemplateLoader configuration;

    private GeoServer gs;

    private final String FEATURES_SERVICE;

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    public TemplateCallBackOGC(GeoServer gs, TemplateLoader configuration) {
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
        this.gs = gs;

        // get the names of the methods that are related to features
        APIService annotation = FeatureService.class.getAnnotation(APIService.class);
        FEATURES_SERVICE = annotation.service();
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        TemplateIdentifier identifier = getTemplateIdentifier(request);
        boolean isOpWithParams = operation != null && operation.getParameters().length > 0;
        if (identifier != null && isOpWithParams) {
            String outputFormat = identifier.getOutputFormat();
            Object param = operation.getParameters()[0];
            String ftName = param != null ? param.toString() : null;
            if ("FEATURES".equalsIgnoreCase(request.getService()) && outputFormat != null) {
                try {
                    FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(ftName);
                    if (typeInfo == null) return operation;
                    RootBuilder root = configuration.getTemplate(typeInfo, outputFormat);
                    String filterLang = (String) request.getKvp().get("FILTER-LANG");
                    String filter = (String) request.getKvp().get("FILTER");
                    if (filter != null && (filterLang == null || filterLang.equalsIgnoreCase("CQL-TEXT"))) {
                        replaceTemplatePathWithFilter(filter, root, typeInfo, operation);
                    }
                    String envParam = request.getRawKvp().get("ENV") != null
                            ? request.getRawKvp().get("ENV").toString()
                            : null;
                    setEnvParameter(envParam);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.operationDispatched(request, operation);
    }

    private void setEnvParameter(String env) {
        if (env != null) {
            try {
                Map<String, Object> localEnvVars = (Map<String, Object>) PARSER.parse(env);
                EnvFunction.setLocalValues(localEnvVars);
            } catch (Exception e) {
                throw new RuntimeException("Invalid syntax for environment variables", e);
            }
        }
    }

    private FeatureTypeInfo getFeatureType(String collectionId) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(collectionId);
        if (featureType == null) {
            throw new ServiceException(
                    "Unknown collection " + collectionId, ServiceException.INVALID_PARAMETER_VALUE, "collectionId");
        }
        return featureType;
    }

    private TemplateIdentifier getTemplateIdentifier(Request request) {
        String accept = request.getHttpRequest().getHeader(HttpHeaders.ACCEPT);
        String format = request.getKvp() != null ? (String) request.getKvp().get("f") : null;
        TemplateIdentifier identifier = null;
        if (format != null) {
            identifier = TemplateIdentifier.fromOutputFormat(format);
        } else if (accept != null) {
            identifier = getTemplateIdentifierFromAcceptString(accept);
        }
        return identifier;
    }

    /**
     * Get the template identifier from the accept header. The accept header can contain multiple comma separated
     * values, so we need to check each one.
     *
     * @param accept the accept header value
     * @return the template identifier or null if not found
     */
    private TemplateIdentifier getTemplateIdentifierFromAcceptString(String accept) {
        if (StringUtils.isBlank(accept)) return null;
        String[] split = accept.split(",");
        for (String s : split) {
            TemplateIdentifier identifier = TemplateIdentifier.fromOutputFormat(s.trim());
            if (identifier != null) {
                return identifier;
            }
        }
        return null;
    }

    private void replaceTemplatePathWithFilter(
            String strFilter, RootBuilder root, FeatureTypeInfo typeInfo, Operation operation) throws Exception {
        if (root != null) {
            replaceFilter(strFilter, root, typeInfo, operation);
        }
    }

    private void replaceFilter(String strFilter, RootBuilder root, FeatureTypeInfo typeInfo, Operation operation)
            throws IOException, CQLException {
        // Get filter from string in order to make it accept the visitor
        Filter filter = XCQL.toFilter(strFilter);
        replaceFilter(filter, root, typeInfo, operation);
    }

    private void replaceFilter(Filter filter, RootBuilder root, FeatureTypeInfo typeInfo, Operation operation)
            throws IOException, CQLException {
        TemplatePathVisitor visitor = new TemplatePathVisitor(typeInfo.getFeatureType());
        // Get filter from string in order to make it accept the visitor
        Filter f = (Filter) filter.accept(visitor, root);
        if (filter.equals(f))
            LOGGER.warning("Failed to resolve filter "
                    + filter
                    + " against the template. If the property name was intended to be a template path, "
                    + "check that the path specified in the cql filter is correct.");
        List<Filter> templateFilters = new ArrayList<>();
        templateFilters.addAll(visitor.getFilters());
        if (templateFilters != null && templateFilters.size() > 0) {
            templateFilters.add(f);
            f = FF.and(templateFilters);
        }
        // Taking back a string from the Filter cause
        // OGC API get a string cql filter from query string
        String newFilter = fixPropertyNames(ECQL.toCQL(f));
        newFilter = TemplateCQLManager.quoteXpathAttribute(newFilter);
        // replace the filter in the operation
        Object[] operationParameters = operation.getParameters();
        operationParameters[6] = newFilter;
    }

    // since the toCQL method quotes the propertynames and that
    // they need to be provided to the ogcapi controller with dot separator
    // this methods remove the quotes and replace slash with dots.
    private String fixPropertyNames(String cqlFilter) {
        Map<String, String> replacements = new HashMap<>();
        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(cqlFilter);
        while (m.find()) {
            String matched = m.group(1);
            String quoted = "\"" + matched + "\"";
            replacements.put(quoted, matched.replaceAll("/", "."));
        }
        for (String toReplace : replacements.keySet()) {
            String replacement = replacements.get(toReplace);
            cqlFilter = cqlFilter.replace(toReplace, replacement);
        }
        return cqlFilter;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        TemplateIdentifier identifier = getTemplateIdentifier(request);
        // we want to override Features API method that are returning a list of features, and
        // when the output format is JSON or GeoJSON or GML
        if ((identifier != null && !identifier.equals(TemplateIdentifier.JSONLD))
                && request.getService().equals(FEATURES_SERVICE)
                && result instanceof FeaturesResponse) {
            FeatureTypeInfo typeInfo = getFeatureType((String) operation.getParameters()[0]);
            if (typeInfo != null) {
                try {
                    RootBuilder root = configuration.getTemplate(typeInfo, identifier.getOutputFormat());
                    if (root != null) {
                        Response templateResp = getTemplateResponse(operation, result, identifier);
                        if (templateResp != null) response = templateResp;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return response;
    }

    private Response getTemplateResponse(Operation operation, Object result, TemplateIdentifier identifier) {
        BaseTemplateGetFeatureResponse templatingResp = null;
        switch (identifier) {
            case JSON:
            case GEOJSON:
                templatingResp = new GeoJSONTemplateGetFeatureResponse(gs, configuration, identifier) {
                    @Override
                    protected void write(
                            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
                            throws ServiceException {
                        FeaturesResponse fr = (FeaturesResponse) result;
                        super.write(fr.getResponse(), output, operation);
                    }
                };
                break;
            case GML32:
                templatingResp = new GMLTemplateResponse(gs, configuration, identifier) {
                    @Override
                    protected void write(
                            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
                            throws ServiceException {
                        FeaturesResponse fr = (FeaturesResponse) result;
                        super.write(fr.getResponse(), output, operation);
                    }
                };
                break;

            case HTML:
                templatingResp = new HTMLTemplateResponse(gs, configuration) {
                    @Override
                    protected void write(
                            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
                            throws ServiceException {
                        FeaturesResponse fr = (FeaturesResponse) result;
                        super.write(fr.getResponse(), output, operation);
                    }
                };
                break;
        }

        return templatingResp;
    }
}
