/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.internal.atom;

import javax.xml.namespace.QName;

import org.geotools.xml.XSD;

/**
 * Atom (<a href="http://tools.ietf.org/html/rfc4287"/>rfc4287</a>) 1.1 XML names
 * 
 * @author Gabriel Roldan
 * 
 */
public class GeoRSS extends XSD {
    public static final String NAMESPACE = "http://www.georss.org/georss";

    public static final QName where = new QName(NAMESPACE, "where");

    @Override
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    @Override
    public String getSchemaLocation() {
        throw new UnsupportedOperationException("We don't know where to get an XSD for atom from");
    }

}
