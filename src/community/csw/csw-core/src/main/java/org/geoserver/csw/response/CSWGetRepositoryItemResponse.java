/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.config.GeoServer;
import org.geoserver.csw.GetRepositoryItemBean;
import org.geoserver.csw.RepositoryItem;
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
        if (request instanceof GetRepositoryItemBean) {
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
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {

        InputStream input = ((RepositoryItem) value).getContents();

        if (null != input) {
            try {
                byte[] buffer = new byte[1024]; // Adjust if you want
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {

            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                } catch (Exception e) {
                    // Nothing to do
                }

                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (Exception e) {
                    // Nothing to do
                }

            }
        }
    }

}
