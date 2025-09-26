/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ogcapi;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.schema.SchemaLoader;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/** A dispatcher callback that hooks into OGC API - Features requests to load and manage schemas for collections */
public class SchemaCallBackOGC extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(SchemaCallBackOGC.class);

    private final Catalog catalog;
    private final SchemaLoader schemaLoader;

    public SchemaCallBackOGC(GeoServer geoServer, SchemaLoader schemaLoader) {
        this.catalog = geoServer.getCatalog();
        this.schemaLoader = schemaLoader;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        boolean isOgcApiQueryables = isIsOgcApiQueryables(request);
        if (!isOgcApiQueryables) return result;
        LOGGER.info("Features getQueryables operation executed, checking for schema override");
        TemplateIdentifier templateIdentifier = TemplateCallBackOGC.getTemplateIdentifier(request);
        boolean isOpWithParams = operation != null
                && operation.getParameters().length > 0
                && operation.getParameters()[0] != null;
        if (templateIdentifier == null || !isOpWithParams) {
            return result;
        }
        String ftName = operation.getParameters()[0].toString();
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(ftName);
        if (typeInfo == null) {
            LOGGER.warning("No FeatureTypeInfo found for name: "
                    + ftName
                    + ", cannot load schema for OGC API - Features request");
            return result;
        }
        String outputFormat = templateIdentifier.getOutputFormat();
        try {
            String schema = schemaLoader.getSchema(typeInfo, outputFormat);
            if (schema != null) {
                return new OGCSchemaOverride(outputFormat, schema);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return super.operationExecuted(request, operation, result);
    }

    private static boolean isIsOgcApiQueryables(Request request) {
        return "Features".equalsIgnoreCase(request.getService())
                && "getQueryables".equalsIgnoreCase(request.getRequest());
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        boolean isOgcApiQueryables = isIsOgcApiQueryables(request);
        if (!isOgcApiQueryables) return response;
        LOGGER.info("Features getQueryables response intercepted, checking for schema override");
        TemplateIdentifier templateIdentifier = TemplateCallBackOGC.getTemplateIdentifier(request);
        boolean isOpWithParams = operation != null && operation.getParameters().length > 0;
        if (templateIdentifier == null || !isOpWithParams) {
            return response;
        }
        Object param = operation.getParameters()[0];
        String ftName = param != null ? param.toString() : null;
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(ftName);
        if (typeInfo == null) {
            LOGGER.warning("No FeatureTypeInfo found for name: "
                    + ftName
                    + ", cannot load schema for OGC API - Features request");
            return response;
        }
        String outputFormat = templateIdentifier.getOutputFormat();
        return getTemplateFeatureResponse(typeInfo, outputFormat);
    }

    private Response getTemplateFeatureResponse(FeatureTypeInfo typeInfos, String outputFormat) {
        Response response = null;
        try {
            String schema = schemaLoader.getSchema(typeInfos, outputFormat);
            if (schema == null) {
                return null;
            }
            return new SchemaOverrideOGCResponse(outputFormat);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** A response that overrides the schema for OGC API - Features requests, providing the schema content directly. */
    static class SchemaOverrideOGCResponse extends Response {

        private final String outputFormat;

        public SchemaOverrideOGCResponse(String outputFormat) {
            super(String.class, outputFormat);
            this.outputFormat = outputFormat;
        }

        @Override
        public String getMimeType(Object value, Operation operation) {
            return outputFormat;
        }

        @Override
        public void write(Object value, OutputStream output, Operation operation) throws IOException, ServiceException {
            if (value == null) {
                throw new ServiceException("No schema content provided for OGC API - Features response");
            }
            OGCSchemaOverride schemaOverride = (OGCSchemaOverride) value;
            output.write(schemaOverride.getContent().getBytes(StandardCharsets.UTF_8));
        }
    }
}
