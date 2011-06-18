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

package org.geotools.xacml.geoxacml.attr;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Christian Mueller
 * 
 *         Factory for obtaining GML2 or GML3 Support
 * 
 */
public class GMLSupportFactory {

    /**
     * @param attr
     *            GeometryAttribute
     * @return GML2Support or GML3Support, depending on attr
     * 
     */
    static public GMLSupport getGMLSupport(GeometryAttribute attr) {
        if (attr.getGmlVersion() == GMLVersion.Version2)
            return GML2Support.Singleton;
        if (attr.getGmlVersion() == GMLVersion.Version3)
            return GML3Support.Singleton;
        return null;
    }

    /**
     * @param node
     *            a gml node.
     * @return GML2Support or GML3Support
     * 
     *         According to the GeoXACML specification an implemntation must be able to parse GML2
     *         and GML3 geometries.
     * 
     *         This method has to take a deeper look into the gml tree to return the proper
     *         GMLSupport.
     * 
     */
    static public GMLSupport getGMLSupport(Node node) {
        if ("Box".equals(node.getLocalName()))
            return GML2Support.Singleton;
        if ("MultiLineString".equals(node.getLocalName()))
            return GML2Support.Singleton;
        if ("MultiPolygon".equals(node.getLocalName()))
            return GML2Support.Singleton;

        if ("Envelope".equals(node.getLocalName()))
            return GML3Support.Singleton;
        if ("MultiCurve".equals(node.getLocalName()))
            return GML3Support.Singleton;
        if ("MultiSurface".equals(node.getLocalName()))
            return GML3Support.Singleton;

        if ("Polygon".equals(node.getLocalName())) {
            if (gmlElemExists(node, "exterior"))
                return GML3Support.Singleton;
            if (gmlElemExists(node, "outerBoundaryIs"))
                return GML2Support.Singleton;
        }

        if (gmlElemExists(node, "posList") || gmlElemExists(node, "pos"))
            return GML3Support.Singleton;
        else
            return GML2Support.Singleton;
    }

    static boolean gmlElemExists(Node n, String elemName) {
        if (n.getNamespaceURI() != null && (GMLSupport.GMLNS.equals(n.getNamespaceURI()))) {
            if (elemName.equals(n.getLocalName()))
                return true;
        }
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (gmlElemExists(nl.item(i), elemName))
                return true;
        }
        return false;
    }

}
