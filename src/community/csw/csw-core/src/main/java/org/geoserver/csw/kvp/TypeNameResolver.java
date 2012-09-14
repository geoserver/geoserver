/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.kvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Helper class putting togheter type names and namespaces to build a list of {@link QName} objects
 * 
 * @author Andrea Aime - GeoSolutions
 */
class TypeNameResolver {

    /**
     * Parses the type names into a list of {@link QName}
     * 
     * @param typenameString a comma separated value of type names
     *        prefix:typeName,prefix:typeName,...
     * 
     * @param namespaces Binds prefixes with namespace URIs
     * @return
     * @throws Exception
     */
    List<QName> parseQNames(String typenameString, NamespaceSupport namespaces) throws Exception {
        // avoid NPE, namespaces is not required
        if(namespaces == null) {
            namespaces = new NamespaceSupport();
        }
        
        List<String> typeNames = (List<String>) new FlatKvpParser("nullKey", String.class)
                .parse(typenameString);
        List<QName> result = new ArrayList<QName>();
        for (String tn : typeNames) {
            int idx = tn.indexOf(":");
            String prefix = null;
            String uri;
            String typeName;
            if (idx == -1) {
                typeName = tn;
                // see if we have a default namespace
                uri = namespaces.getURI("");
                if (uri == null) {
                    throw new ServiceException("Type name " + tn
                            + " has no prefix, but there is no default prefix "
                            + "declared in the NAMESPACE parameter",
                            ServiceException.INVALID_PARAMETER_VALUE, "typename");
                }
            } else {
                typeName = tn.substring(idx + 1);
                prefix = tn.substring(0, idx);
                uri = namespaces.getURI(prefix);
                if (uri == null) {
                    uri = CSWRecordDescriptor.NAMESPACES.getURI(prefix);
                    if (uri == null) {
                        throw new ServiceException("Type name " + tn
                                + " has an unknown prefix, please qualify it using the "
                                + "NAMESPACE paramter, or use a well known prefix: "
                                + ServiceException.INVALID_PARAMETER_VALUE, "typename");
                    }
                }
            }

            QName qname = null;
            if(prefix != null) {
                qname = new QName(uri, typeName, prefix);
            } else {
                qname = new QName(uri, typeName);
            }
            result.add(qname);
        }

        return result;
    }
}
