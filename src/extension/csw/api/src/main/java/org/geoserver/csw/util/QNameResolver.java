/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.util;

import javax.xml.namespace.QName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Helper class turning a qualified names as a string and namespaces into a QName object
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QNameResolver {

    /**
     * Parses the string into a list of {@link QName}
     *
     * @param qualifiedString a string in the form prefix:localName
     * @param namespaces Binds prefixes with namespace URIs
     */
    public QName parseQName(String qualifiedString, NamespaceSupport namespaces) {
        int idx = qualifiedString.indexOf(":");
        String prefix = null;
        String uri;
        String typeName;
        if (idx == -1) {
            typeName = qualifiedString;
            // see if we have a default namespace
            uri = namespaces.getURI("");
        } else {
            typeName = qualifiedString.substring(idx + 1);
            prefix = qualifiedString.substring(0, idx);
            uri = namespaces.getURI(prefix);
        }

        QName qname = null;
        if (prefix != null) {
            qname = new QName(uri, typeName, prefix);
        } else {
            qname = new QName(uri, typeName);
        }
        return qname;
    }
}
