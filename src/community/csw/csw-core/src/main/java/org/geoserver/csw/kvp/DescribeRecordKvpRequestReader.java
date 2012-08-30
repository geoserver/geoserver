/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.DescribeRecordType;

import org.xml.sax.helpers.NamespaceSupport;

/**
 * DescribeRecord KVP request reader
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class DescribeRecordKvpRequestReader extends CSWKvpRequestReader {

    TypeNameResolver resolver = new TypeNameResolver();

    public DescribeRecordKvpRequestReader() {
        super(DescribeRecordType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // at this point the namespace and type names are separated, we need to merge them and build
        // QNames
        String typename = (String) kvp.get("typename");
        NamespaceSupport namespaces = (NamespaceSupport) kvp.get("namespace");
        
        List<QName> qnames = resolver.parseQNames(typename, namespaces);
        kvp.put("typename", qnames);

        // proceed with the normal reflective setup
        return super.read(request, kvp, rawKvp);
    }

}
