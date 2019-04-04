/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import javax.xml.namespace.QName;
import org.geoserver.csw.util.QNameResolver;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Extends a propertyName representing a SimpleLiteral adding /dc:value at its end, and fixing the
 * namespace support as necessary
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWPropertyPathExtender {

    QNameResolver resolver = new QNameResolver();

    PropertyName extendProperty(
            PropertyName expression, FilterFactory2 filterFactory, NamespaceSupport nss) {
        String path = expression.getPropertyName();
        if (nss != null) {
            QName name = resolver.parseQName(path, nss);
            String uri = name.getNamespaceURI();
            if (uri != null && !"".equals(uri)) {
                if (DC.NAMESPACE.equals(uri) || DCT.NAMESPACE.equals(uri)) {
                    path = path + "/dc:value";
                }
            } else {
                AttributeDescriptor descriptor = CSWRecordDescriptor.getDescriptor(path);
                if (descriptor != null) {
                    if (DC.NAMESPACE.equals(descriptor.getName().getNamespaceURI())) {
                        path = "dc:" + path + "/dc:value";
                        nss = CSWRecordDescriptor.NAMESPACES;
                    } else if (DCT.NAMESPACE.equals(descriptor.getName().getNamespaceURI())) {
                        path = "dct:" + path + "/dc:value";
                        nss = CSWRecordDescriptor.NAMESPACES;
                    }
                }
            }
        }

        return filterFactory.property(path, nss);
    }
}
