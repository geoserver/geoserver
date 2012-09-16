/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.csw.util.QNameResolver;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Expands the paths referring to SimpleLiteral instances so that they contain the dc:value ending
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class SimpleLiteralPathExtender extends DuplicatingFilterVisitor {
    
    QNameResolver resolver = new QNameResolver();
    
    public SimpleLiteralPathExtender(NamespaceSupport defaultNss) {
        super();
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        NamespaceSupport nss = expression.getNamespaceContext();
        String path = expression.getPropertyName();
        if(nss != null) {
            QName name  = resolver.parseQName(expression.getPropertyName(), nss);
            String uri = name.getNamespaceURI();
            if(uri != null && !"".equals(uri)) {
                if(DC.NAMESPACE.equals(uri) || DCT.NAMESPACE.equals(uri)) {
                    path = path + "/dc:value";
                }
            } else {
                AttributeDescriptor descriptor = CSWRecordDescriptor.getDescriptor(path);
                if(descriptor != null) {
                    if(DC.NAMESPACE.equals(descriptor.getName().getNamespaceURI())) {
                        path = "dc:" + path + "/dc:value";
                        nss = CSWRecordDescriptor.NAMESPACES;
                    } else if(DCT.NAMESPACE.equals(descriptor.getName().getNamespaceURI())) {
                        path = "dct:" + path + "/dc:value";
                        nss = CSWRecordDescriptor.NAMESPACES;
                    }
                }
            }
        }
        return getFactory(extraData).property(path, nss);
    }
}
