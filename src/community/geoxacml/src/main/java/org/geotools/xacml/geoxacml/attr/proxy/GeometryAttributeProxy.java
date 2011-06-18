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

package org.geotools.xacml.geoxacml.attr.proxy;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.w3c.dom.Node;

import com.sun.xacml.attr.AttributeProxy;
import com.sun.xacml.attr.AttributeValue;

/**
 * @author Christian Mueller
 * 
 *         A proxy class, this is required by the SUN XACML implementation
 */
public class GeometryAttributeProxy implements AttributeProxy {

    public AttributeValue getInstance(Node root) throws Exception {
        return GeometryAttribute.getInstance(root);
    }

    public AttributeValue getInstance(String value) throws Exception {
        return GeometryAttribute.getInstance(value);
    }

}
