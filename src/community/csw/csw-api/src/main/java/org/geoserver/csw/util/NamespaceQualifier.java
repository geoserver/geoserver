/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.util;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Adds the propert namespace support to PropertyName instances lacking it, and expands
 * the paths referring to SimpleLiteral instances so that they contain the dc:value ending
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class NamespaceQualifier extends DuplicatingFilterVisitor {

    NamespaceSupport defaultNss;

    public NamespaceQualifier(NamespaceSupport defaultNss) {
        super();
        this.defaultNss = defaultNss;
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        NamespaceSupport nss = expression.getNamespaceContext();
        if (nss == null) {
            nss = defaultNss;
        }
        return getFactory(extraData).property(expression.getPropertyName(), nss);
    }
}
