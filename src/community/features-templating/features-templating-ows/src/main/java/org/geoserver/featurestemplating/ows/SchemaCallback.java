/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.configuration.schema.SchemaLoader;
import org.geoserver.featurestemplating.ows.wfs.SchemaOverrideDescribeFeatureTypeResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;

/**
 * This {@link DispatcherCallback} implementation checks on operation dispatched event if a json-ld path has been
 * provided to cql_filter and evaluate it against the {@link TemplateBuilder} tree to get the corresponding
 * {@link Filter}
 */
public class SchemaCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(SchemaCallback.class);

    private Catalog catalog;

    private GeoServer gs;

    private SchemaLoader configuration;

    static FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    public SchemaCallback(GeoServer gs, SchemaLoader configuration) {
        this.gs = gs;
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return super.operationDispatched(request, operation);
    }

    // get the FeatureTypeInfo from the query
    private List<FeatureTypeInfo> getFeatureTypeInfoFromQuery(Query q) {
        List<FeatureTypeInfo> typeInfos = new ArrayList<>();
        for (QName typeName : q.getTypeNames()) {
            typeInfos.add(catalog.getFeatureTypeByName(new NameImpl(typeName.getPrefix(), typeName.getLocalPart())));
        }
        return typeInfos;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
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
        return "DescribeFeatureType".equalsIgnoreCase(id) && "wfs".equalsIgnoreCase(serviceId);
    }

    private Response findResponse(Object param1) {
        DescribeFeatureTypeRequest dftr = DescribeFeatureTypeRequest.adapt(param1);
        if (dftr == null) {
            LOGGER.log(Level.FINE, "DescribeFeatureTypeRequest not found in operation parameters");
            return null;
        }
        List<QName> qNames = dftr.getTypeNames();
        if (qNames == null || qNames.isEmpty()) {
            LOGGER.log(Level.FINE, "No type names found in DescribeFeatureTypeRequest");
            return null;
        } else if (qNames.size() > 1) {
            LOGGER.log(
                    Level.FINE, "Multiple type names found in DescribeFeatureTypeRequest, only single type supported");
            return null;
        }
        QName qName = qNames.get(0);
        FeatureTypeInfo featureTypeByName = catalog.getFeatureTypeByName(qName.getPrefix(), qName.getLocalPart());
        return getTemplateFeatureResponse(featureTypeByName, dftr.getOutputFormat());
    }

    private Response getTemplateFeatureResponse(FeatureTypeInfo typeInfos, String outputFormat) {
        Response response = null;
        try {
            String schema = configuration.getSchema(typeInfos, outputFormat);
            if (schema == null) {
                return null;
            }
            response = new SchemaOverrideDescribeFeatureTypeResponse(gs, outputFormat, schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
