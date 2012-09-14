/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.RequestBaseType;

import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.opengis.feature.type.FeatureType;

/**
 * Base class for CSW record responses
 * 
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractRecordsResponse extends Response {

    String schema;

    GeoServer gs;

    FeatureType recordType;

    public AbstractRecordsResponse(FeatureType recordType, String schema, GeoServer gs) {
        super(CSWRecordsResult.class);
        this.schema = schema;
        this.gs = gs;
        this.recordType = recordType;
    }

    @Override
    public boolean canHandle(Operation operation) {
        String requestedSchema = getRequestedSchema(operation);
        if(requestedSchema == null) {
            requestedSchema = CSW.NAMESPACE;
        }
        return requestedSchema.equals(schema);
    }

    private String getRequestedSchema(Operation operation) {
        Object request = operation.getParameters()[0];
        if (request instanceof GetRecordByIdType) {
            GetRecordByIdType gr = (GetRecordByIdType) request;
            return gr.getOutputSchema();
        } else if (request instanceof GetRecordsType) {
            GetRecordsType gr = (GetRecordsType) request;
            return gr.getOutputSchema();
        } else {
            throw new IllegalArgumentException("Unsupported request object type: " + request);
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        CSWRecordsResult result = (CSWRecordsResult) value;
        RequestBaseType request = (RequestBaseType) operation.getParameters()[0];
        CSWInfo csw = gs.getService(CSWInfo.class);

        // if this is a hit request we don't have records to encode
        if(result.getRecords() != null) {
            FeatureType recordSchema = result.getRecords().getSchema();
            if (!recordType.equals(recordSchema)) {
                throw new IllegalArgumentException("Cannot encode this kind of record "
                        + recordSchema.getName() + " into schema " + schema);
            }
        }

        transformResponse(output, result, request, csw);
    }

    /**
     * Actually encodes the response into a set of records
     * 
     * @param output
     * @param result
     * @param request
     * @param csw
     */
    protected abstract void transformResponse(OutputStream output, CSWRecordsResult result,
            RequestBaseType request, CSWInfo csw);
}
