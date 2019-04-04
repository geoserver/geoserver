/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.cat.csw20.DescribeRecordType;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * DescribeRecord KVP request reader
 *
 * @author Andrea Aime, GeoSolutions
 */
public class DescribeRecordKvpRequestReader extends CSWKvpRequestReader {

    TypeNamesResolver resolver = new TypeNamesResolver();

    public DescribeRecordKvpRequestReader() {
        super(DescribeRecordType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // at this point the namespace and type names are separated, we need to merge them and build
        // QNames
        String typename = (String) rawKvp.get("typename");
        if (typename != null) {
            NamespaceSupport namespaces = (NamespaceSupport) kvp.get("namespace");
            if (namespaces == null) {
                // when null the default is the CSW one
                namespaces = CSWRecordDescriptor.NAMESPACES;
            }

            List<QName> qnames = resolver.parseQNames(typename, namespaces);
            kvp.put("typename", qnames);
        }

        // proceed with the normal reflective setup
        return super.read(request, kvp, rawKvp);
    }
}
