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

import net.opengis.cat.csw20.DescribeRecordType;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * DescribeRecord KVP request reader
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class DescribeRecordKvpRequestReader extends CSWKvpRequestReader {

    static final Map<String, String> COMMON_PREFIXES = new HashMap<String, String>() {
        {
            put("csw", CSW.NAMESPACE);
            put("rim", "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0");
            // TODO: add the
        }
    };

    public DescribeRecordKvpRequestReader() {
        super(DescribeRecordType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // at this point the namespace and type names are separated, we need to merge them and build
        // QNames
        String typename = (String) kvp.get("typename");
        NamespaceSupport namespaces = (NamespaceSupport) kvp.get("namespace");
        if (namespaces == null) {
            namespaces = new NamespaceSupport();
        }
        List<QName> qnames = parseTypeNames(typename, namespaces);
        kvp.put("typename", qnames);

        // proceed with the normal reflective setup
        return super.read(request, kvp, rawKvp);
    }

    private List<QName> parseTypeNames(String typenameString, NamespaceSupport namespaces)
            throws Exception {
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
                    uri = COMMON_PREFIXES.get(prefix);
                    if (uri == null) {
                        throw new ServiceException("Type name " + tn
                                + " has an unknown prefix, please qualify it using the "
                                + "NAMESPACE paramter, or use a well known prefix: "
                                + ServiceException.INVALID_PARAMETER_VALUE, "typename");
                    }
                }
            }

            QName qname = new QName(uri, typeName, prefix);
            result.add(qname);
        }

        return result;
    }

}
