/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.capabilities;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Base class that handles common behavior between 1.1.1 and 1.3.0
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
public abstract class BaseCapabilitiesResponse extends Response {

    private String mime;

    /**
     * @param binding
     */
    protected BaseCapabilitiesResponse(Class<?> binding, String mime) {
        super(binding);
        this.mime=mime;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return "getcapabilities_"+operation.getService().getVersion().toString()+".xml";
    }

    /**
     * @return {@code "text/xml"}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {
    
        if (value.getClass().isAssignableFrom(super.getBinding())) {
            return mime;
        }
    
        throw new IllegalArgumentException(value == null ? "null" : value.getClass().getName()
                + "/" + operation.getId());
    }

}