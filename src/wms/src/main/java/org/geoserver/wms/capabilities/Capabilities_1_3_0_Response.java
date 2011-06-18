/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetCapabilitiesRequest;

/**
 * OWS {@link Response} bean to handle WMS {@link GetCapabilities} results
 * 
 * @author groldan
 * 
 */
public class Capabilities_1_3_0_Response extends Response {

    public Capabilities_1_3_0_Response() {
        super(Capabilities_1_3_0_Transformer.class);
    }

    /**
     * @return {@code "application/vnd.ogc.wms_xml"}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {

        if (value instanceof Capabilities_1_3_0_Transformer) {
            return Capabilities_1_3_0_Transformer.WMS_CAPS_MIME;
        }

        throw new IllegalArgumentException(value == null ? "null" : value.getClass().getName()
                + "/" + operation.getId());
    }

    /**
     * @param value
     *            {@link Capabilities_1_3_0_Transformer}
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

        Capabilities_1_3_0_Transformer transformer = (Capabilities_1_3_0_Transformer) value;

        try {
            GetCapabilitiesRequest request = (GetCapabilitiesRequest) operation.getParameters()[0];
            transformer.transform(request, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

}
