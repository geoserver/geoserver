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
public class Atom extends XSD {

    public static final String DEFAULT_PREFIX = "atom";

    public static final String NAMESPACE = "http://www.w3.org/2005/Atom";

    public static QName feed = new QName(NAMESPACE, "feed");

    public static QName title = new QName(NAMESPACE, "title");

    public static QName subtitle = new QName(NAMESPACE, "subtitle");

    public static QName link = new QName(NAMESPACE, "link");

    public static QName rights = new QName(NAMESPACE, "rights");

    public static QName generator = new QName(NAMESPACE, "generator");

    public static QName updated = new QName(NAMESPACE, "updated");

    public static QName author = new QName(NAMESPACE, "author");

    public static QName name = new QName(NAMESPACE, "name");

    public static QName uri = new QName(NAMESPACE, "uri");

    public static QName email = new QName(NAMESPACE, "email");

    public static QName contributor = new QName(NAMESPACE, "contributor");

    public static QName id = new QName(NAMESPACE, "id");

    public static QName entry = new QName(NAMESPACE, "entry");

    public static QName summary = new QName(NAMESPACE, "summary");

    public static QName content = new QName(NAMESPACE, "content");

    public static QName category = new QName(NAMESPACE, "category");

    public static QName term = new QName(NAMESPACE, "term");

    public static QName scheme = new QName(NAMESPACE, "scheme");

    public static QName version = new QName(NAMESPACE, "version");

    public static QName icon = new QName(NAMESPACE, "icon");

    public static QName href = new QName(NAMESPACE, "href");

    public static QName rel = new QName(NAMESPACE, "rel");

    public static QName type = new QName(NAMESPACE, "type");

    public static QName hreflang = new QName(NAMESPACE, "hreflang");

    public static QName length = new QName(NAMESPACE, "length");

    public static QName published = new QName(NAMESPACE, "published");

    public static QName source = new QName(NAMESPACE, "source");

    public static QName typeName = new QName(NAMESPACE, "typeName");

    @Override
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    @Override
    public String getSchemaLocation() {
        throw new UnsupportedOperationException("We don't know where to get an XSD for atom from");
    }

}
