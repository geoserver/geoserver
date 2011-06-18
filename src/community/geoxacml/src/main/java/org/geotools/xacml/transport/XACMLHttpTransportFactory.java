/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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

package org.geotools.xacml.transport;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory creating transport objects for a remote PDP using http POST requests
 * 
 * @author Mueller Christian
 * 
 */
public class XACMLHttpTransportFactory implements XACMLTransportFactory {

    XACMLTransport transport;

    public XACMLHttpTransportFactory(String urlString, boolean multithreaded) {
        URL pdpURL = null;
        try {
            pdpURL = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        transport = new XACMLHttpTransport(pdpURL, multithreaded);
    }

    public XACMLTransport getXACMLTransport() {
        return transport;
    }

}
