/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.internal.opensearch;

import javax.xml.namespace.QName;

import org.geotools.xml.XSD;

/**
 * OpenSearch 1.1 XML names
 * 
 * @author Gabriel Roldan
 * 
 */
public class OS extends XSD {

    public static final String DEFAULT_PREFIX = "os";

    public static final String NAMESPACE = "http://a9.com/-/spec/opensearch/1.1/";

    public static final QName OpenSearchDescription = new QName(NAMESPACE, "OpenSearchDescription",
            DEFAULT_PREFIX);

    public static final QName ShortName = new QName(NAMESPACE, "ShortName", DEFAULT_PREFIX);

    public static final QName Description = new QName(NAMESPACE, "Description", DEFAULT_PREFIX);

    public static final QName Tags = new QName(NAMESPACE, "Tags", DEFAULT_PREFIX);

    public static final QName Contact = new QName(NAMESPACE, "Contact", DEFAULT_PREFIX);

    public static final QName Url = new QName(NAMESPACE, "Url", DEFAULT_PREFIX);

    public static final QName LongName = new QName(NAMESPACE, "LongName", DEFAULT_PREFIX);

    public static final QName Image = new QName(NAMESPACE, "Image", DEFAULT_PREFIX);

    public static final QName Query = new QName(NAMESPACE, "Query", DEFAULT_PREFIX);

    public static final QName Developer = new QName(NAMESPACE, "Developer", DEFAULT_PREFIX);

    public static final QName Attribution = new QName(NAMESPACE, "Attribution", DEFAULT_PREFIX);

    public static final QName SyndicationRight = new QName(NAMESPACE, "SyndicationRight",
            DEFAULT_PREFIX);

    public static final QName AdultContent = new QName(NAMESPACE, "AdultContent", DEFAULT_PREFIX);

    public static final QName Language = new QName(NAMESPACE, "Language", DEFAULT_PREFIX);

    public static final QName OutputEncoding = new QName(NAMESPACE, "OutputEncoding",
            DEFAULT_PREFIX);

    public static final QName InputEncoding = new QName(NAMESPACE, "InputEncoding", DEFAULT_PREFIX);

    @Override
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    @Override
    public String getSchemaLocation() {
        return getClass().getResource("schemas/os/1.1/OpenSearch.xsd").toString();
    }

}
