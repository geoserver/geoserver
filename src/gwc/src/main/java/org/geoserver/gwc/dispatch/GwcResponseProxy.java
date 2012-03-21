/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * The configured GeoServer Dispatcher {@link Response} object that writes down the contents of the
 * {@link GwcOperationProxy} response produced by a {@link GwcServiceProxy}.
 * <p>
 * See the package documentation for more insights on how these all fit together.
 * 
 */
public class GwcResponseProxy extends Response {

    public GwcResponseProxy() {
        super(GwcOperationProxy.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {

        GwcOperationProxy op = (GwcOperationProxy) value;
        String mimeType = op.getMimeType();
        return mimeType;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {

        GwcOperationProxy op = (GwcOperationProxy) value;
        byte[] contents = op.getContents();
        output.write(contents);
    }

}
