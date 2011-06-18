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

import java.util.List;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * Since XACML says nothing about the communication between PEP and PDP, this interface offers the
 * possibility to implement different means of communication (local, remote using http,....)
 * 
 * @author Christian Mueller
 * 
 */
public interface XACMLTransport {

    public List<ResponseCtx> evaluateRequestCtxList(List<RequestCtx> requests);

    public ResponseCtx evaluateRequestCtx(RequestCtx request);

}
