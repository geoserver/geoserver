/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.request.TemplatePathVisitor;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
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

    private TemplateLoader configuration;

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    public TemplateCallback(GeoServer gs, TemplateLoader configuration) {
        this.gs = gs;
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if (operationSupported(operation)) {
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
                                    + ECQL.toCQL(old)
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
        if (operationSupported(operation) && params.length > 0) {
            Object param1 = params[0];
            Response replacer = findResponse(param1);
            if (replacer != null) response = replacer;
        }
        return super.responseDispatched(request, operation, result, response);
    }

    /**
     * Checks if operation is supported by features templating
     *
     * @param operation
     * @return
     */
    private boolean operationSupported(Operation operation) {
        String id = operation.getId();
        String serviceId = operation.getService().getId();
        return "GetFeatureInfo".equalsIgnoreCase(id) && "wms".equalsIgnoreCase(serviceId)
                || "GetFeature".equalsIgnoreCase(id) && "wfs".equalsIgnoreCase(serviceId);
    }

    private Response findResponse(Object param1) {
        Response response = null;
        if (param1 instanceof GetFeatureInfoRequest) {
            GetFeatureInfoRequest request = (GetFeatureInfoRequest) param1;
            if (request.getInfoFormat() != null) {
                response = getTemplateFeatureInfoResponse(request);
            }
        } else {
            GetFeatureRequest getFeature = GetFeatureRequest.adapt(param1);
            if (getFeature != null) {
                List<Query> queries = getFeature.getQueries();
                for (Query q : queries) {
                    List<FeatureTypeInfo> typeInfos = getFeatureTypeInfoFromQuery(q);
                    Response templateResponse =
                            getTemplateFeatureResponse(typeInfos, getFeature.getOutputFormat());
                    if (templateResponse != null) response = templateResponse;
                }
            }
        }
        return response;
    }

    /**
     * Helper method to find the response for a WMS GetFeatureInfo request.
     *
     * <p>If none of the requested layers uses a feature template for the requested info format,
     * then we delegate on whatever is the default response for the requested info format.
     *
     * <p>If at least one of the requested layers uses a feature template for the requested info
     * format, then all the requested layers need to have a matching feature template for the
     * requested info format, otherwise an exception will be thrown.
     */
    private Response getTemplateFeatureInfoResponse(GetFeatureInfoRequest request) {
        String infoFormat = request.getInfoFormat();
        TemplateIdentifier identifier =
                TemplateIdentifier.fromOutputFormat(request.getInfoFormat());
        if (identifier == null) {
            // features templating doesn't support the requested info type,
            // hence we delegate on whatever is the default response
            return null;
        }
        // the request info format is supported by features templating
        // let's check if the requested layers all have a template supporting
        // the requested info format
        List<MapLayerInfo> layers = request.getQueryLayers();
        int matchingTemplates = 0;
        for (MapLayerInfo layer : layers) {
            if (layer != null
                    && layer.getResource() instanceof FeatureTypeInfo
                    && ensureTemplatesExist(layer.getFeature(), identifier.getOutputFormat())
                            != null) {
                matchingTemplates++;
            }
        }
        // no requested layer is using features templating, hence we delegate in whatever is the
        // default info format
        if (matchingTemplates == 0) {
            return null;
        }
        // if one of the requested layer uses a features template, then all the other need to have
        // one too
        int matchTemplatesDiff = layers.size() - matchingTemplates;
        if (matchTemplatesDiff != 0) {
            // there is at least one requested layer that doesn't have template matching the
            // requested info format
            // this situation is currently not supported
            throw new ServiceException(
                    "To get a features templating getFeatureInfo a template is needed for every FeatureType but "
                            + matchTemplatesDiff
                            + " among the requested ones are missing a template");
        }
        return OWSResponseFactory.getInstance().featureInfoResponse(identifier, infoFormat);
    }

    private Response getTemplateFeatureResponse(
            List<FeatureTypeInfo> typeInfos, String outputFormat) {
        Response response = null;
        try {

            List<RootBuilder> rootBuilders =
                    getRootBuildersFromFeatureTypeInfo(typeInfos, outputFormat);
            if (rootBuilders.size() > 0) {
                TemplateIdentifier templateIdentifier =
                        TemplateIdentifier.fromOutputFormat(outputFormat);
                response = OWSResponseFactory.getInstance().getFeatureResponse(templateIdentifier);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    // get the root builder and eventually throws exception if it is
    // null and output format with mandatory template is requested
    private RootBuilder ensureTemplatesExist(FeatureTypeInfo typeInfo, String outputFormat) {
        try {
            TemplateIdentifier identifier = TemplateIdentifier.fromOutputFormat(outputFormat);
            RootBuilder rootBuilder = null;
            if (identifier != null) {
                rootBuilder = configuration.getTemplate(typeInfo, identifier.getOutputFormat());
                if (templateIsMandatory(identifier) && rootBuilder == null) {
                    throw new RuntimeException(
                            "No template found for feature type "
                                    + typeInfo.getName()
                                    + " for output format "
                                    + outputFormat);
                }
            }
            return rootBuilder;
        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Exception will trying to check the existence of features templates for the requested feature types",
                    e);
        }
    }

    // Check if a template is mandatory for the requested output format and operation.
    // A template is mandatory if the operation does not support the output format requested
    // without a features template. Examples JSON-LD for all operations and HTML for WFS GetFeature.
    private boolean templateIsMandatory(TemplateIdentifier identifier) {
        String requestName = Dispatcher.REQUEST.get().getRequest();
        boolean isFeatureInfo =
                requestName != null && requestName.equalsIgnoreCase("GetFeatureInfo");
        boolean mandatoryHTML = !isFeatureInfo && identifier.equals(TemplateIdentifier.HTML);
        return identifier != null
                && (mandatoryHTML || identifier.equals(TemplateIdentifier.JSONLD));
    }
}
