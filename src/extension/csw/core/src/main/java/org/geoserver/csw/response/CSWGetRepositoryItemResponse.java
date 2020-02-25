/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.GetRepositoryItemType;
import org.geoserver.csw.store.RepositoryItem;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Encodes Repository Item stream
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class CSWGetRepositoryItemResponse extends Response {

    GeoServer gs;

    public CSWGetRepositoryItemResponse(GeoServer gs) {
        super(RepositoryItem.class);
        this.gs = gs;
    }

    @Override
    public boolean canHandle(Operation operation) {
        Object request = operation.getParameters()[0];
        if (request instanceof GetRepositoryItemType) {
            return true;
        } else {
            throw new IllegalArgumentException("Unsupported request object type: " + request);
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        String mimeType = ((RepositoryItem) value).getMime();
        return mimeType;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        try (InputStream input = ((RepositoryItem) value).getContents()) {
            if (null != input) {
                IOUtils.copy(input, output);
            } else {
                throw new HttpErrorCodeException(404, "Repository item had no content");
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to encode the repository item onto the output", e);
        } finally {
            output.flush();
        }
    }
}
