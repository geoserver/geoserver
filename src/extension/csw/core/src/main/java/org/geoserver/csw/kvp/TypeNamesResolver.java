/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.kvp;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.csw.util.QNameResolver;
import org.geoserver.platform.ServiceException;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Helper class putting together type names as strings and namespaces to build a list of {@link
 * QName} objects
 *
 * @author Andrea Aime - GeoSolutions
 */
class TypeNamesResolver {

    QNameResolver resolver = new QNameResolver();

    /**
     * Parses the type names into a list of {@link QName}
     *
     * @param qualifiedString a comma separated value of qualified names prefix:name,prefix:name,...
     * @param namespaces Binds prefixes with namespace URIs
     */
    public List<QName> parseQNames(String qualifiedString, NamespaceSupport namespaces) {
        // simplify the algorithm below so that it does not have to care for NPE
        if (namespaces == null) {
            namespaces = new NamespaceSupport();
        }

        String[] typeNames = qualifiedString.split("\\s*,\\s*");
        List<QName> result = new ArrayList<QName>();
        for (String tn : typeNames) {
            QName qname = resolver.parseQName(tn, namespaces);
            if (qname.getNamespaceURI() == null) {
                throw new ServiceException(
                        "Type name "
                                + tn
                                + " has no prefix, but there is no default prefix "
                                + "declared in the NAMESPACE parameter",
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "typename");
            }

            result.add(qname);
        }

        return result;
    }
}
