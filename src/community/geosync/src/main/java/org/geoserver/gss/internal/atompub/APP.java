/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.internal.atompub;

import javax.xml.namespace.QName;

import org.geotools.xml.XSD;

/**
 * Atom Publishing Protocol (<a href=http://atompub.org/>atompub.org</a>) 1.1 XML names
 * 
 * @author Gabriel Roldan
 * 
 */
public class APP extends XSD {

    public static final String DEFAULT_PREFIX = "app";

    public static final String NAMESPACE = "http://www.w3.org/2007/app";

    public static final QName service = new QName(NAMESPACE, "service");

    public static QName workspace = new QName(NAMESPACE, "workspace");

    public static QName collection = new QName(NAMESPACE, "collection");

    public static QName categories = new QName(NAMESPACE, "categories");

    public static QName accept = new QName(NAMESPACE, "accept");

    @Override
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    @Override
    public String getSchemaLocation() {
        throw new UnsupportedOperationException("We don't know where to get an XSD for atom from");
    }

}
