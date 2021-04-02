/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.request;

import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.feature.NameImpl;

public class SchemalessDispatcherCallback extends AbstractDispatcherCallback {

    private Catalog catalog;

    private static final String GET_FEATURE = "GetFeature";

    private static final String WFS = "WFS";

    public SchemalessDispatcherCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return super.serviceDispatched(request, service);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        String service = request.getService();
        String outputFormat = null;
        Object infoRequest = operation.getParameters()[0];
        boolean allowRequest = true;
        if (infoRequest instanceof GetFeatureInfoRequest) {
            GetFeatureInfoRequest getInfoRequest = (GetFeatureInfoRequest) infoRequest;
            boolean isSchemaless =
                    isSchemalessTypeBeingRequested((GetFeatureInfoRequest) infoRequest);
            outputFormat = getInfoRequest.getInfoFormat();
            allowRequest = !(!isOutputFormatSupported(outputFormat) && isSchemaless);
        } else if (isGetFeatureRequest(service, request.getRequest())) {
            boolean isSchemaless = isSchemalessTypeBeingRequested(operation);
            outputFormat = request.getOutputFormat();
            allowRequest = !(isSchemaless && !isOutputFormatSupported(outputFormat));
        }
        if (!allowRequest)
            throw new UnsupportedOperationException(
                    "Schemaless support for "
                            + request.getRequest()
                            + " is not available for "
                            + outputFormat);

        return super.operationDispatched(request, operation);
    }

    private boolean isSchemalessTypeBeingRequested(Operation operation) {
        GetFeatureRequest getFeatureRequest = GetFeatureRequest.adapt(operation.getParameters()[0]);
        List<Query> queries = getFeatureRequest.getQueries();
        if (getFeatureRequest != null && queries != null && !queries.isEmpty()) {
            for (Query q : queries) {
                if (isSchemalessTypeBeingRequested(q) == true) return true;
            }
        }
        return false;
    }

    // get the FeatureTypeInfo from the query
    private boolean isSchemalessTypeBeingRequested(Query q) {
        for (QName typeName : q.getTypeNames()) {
            FeatureTypeInfo featureTypeInfo =
                    catalog.getFeatureTypeByName(
                            new NameImpl(typeName.getPrefix(), typeName.getLocalPart()));
            try {
                if (featureTypeInfo.getFeatureType() instanceof DynamicFeatureType) return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private boolean isGetFeatureRequest(String service, String method) {
        if (service.equalsIgnoreCase(WFS) && method.equalsIgnoreCase(GET_FEATURE)) return true;
        return false;
    }

    private boolean isOutputFormatSupported(String outputFormat) {
        if (outputFormat.equalsIgnoreCase("application/json")
                || outputFormat.equalsIgnoreCase("text/html")) return true;
        return false;
    }

    private boolean isSchemalessTypeBeingRequested(GetFeatureInfoRequest getFeatureInfoRequest) {
        List<MapLayerInfo> layerInfos = getFeatureInfoRequest.getQueryLayers();
        for (MapLayerInfo l : layerInfos) {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(l.getName());
            try {
                if (fti != null && fti.getFeatureType() instanceof DynamicFeatureType) {
                    return false;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
