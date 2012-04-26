/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.service.GetCapabilities;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * OWS {@link Response} bean to handle WMS {@link GetCapabilities} results.
 * 
 * 
 */
public class GetCapabilitiesResponse extends Response {

    private GSS gss;

    /**
     */
    public GetCapabilitiesResponse(final GSS gss) {
        super(GetCapabilitiesTransformer.class);
        this.gss = gss;
    }

    /**
     * @return {@code "text/xml"}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {

        if (value instanceof GetCapabilitiesTransformer) {
            return "text/xml";
        }

        throw new IllegalArgumentException(value == null ? "null" : value.getClass().getName()
                + "/" + operation.getId());
    }

    /**
     * @param value
     *            {@link GetCapabilitiesTransformer}
     * @param output
     *            destination
     * @param operation
     *            The operation identifier which resulted in <code>value</code>
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        final GetCapabilitiesTransformer transformer = (GetCapabilitiesTransformer) value;
        final GetCapabilities request = (GetCapabilities) operation.getParameters()[0];

        try {
            transformer.setIndentation(2);
            transformer.setNamespaceDeclarationEnabled(true);
            transformer.transform(request, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

}
