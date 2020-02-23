/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import net.opengis.cat.csw20.GetDomainType;
import net.opengis.cat.csw20.RequestBaseType;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Encodes domain values responses
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWGetDomainResponse extends Response {

    GeoServer gs;

    public CSWGetDomainResponse(GeoServer gs) {
        super(CloseableIterator.class);
        this.gs = gs;
    }

    @Override
    public boolean canHandle(Operation operation) {
        Object request = operation.getParameters()[0];
        if (request instanceof GetDomainType) {
            return true;
        } else {
            throw new IllegalArgumentException("Unsupported request object type: " + request);
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        CloseableIterator<String> result = (CloseableIterator<String>) value;
        RequestBaseType request = (RequestBaseType) operation.getParameters()[0];
        CSWInfo csw = gs.getService(CSWInfo.class);

        transformResponse(output, result, request, csw);
    }

    /** Actually encodes the response into a set of records */
    protected void transformResponse(
            OutputStream output,
            CloseableIterator<String> result,
            RequestBaseType request,
            CSWInfo csw) {
        CSWDomainValuesTransformer transformer =
                new CSWDomainValuesTransformer(request, csw.isCanonicalSchemaLocation());
        try {
            transformer.transform(result, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }
}
