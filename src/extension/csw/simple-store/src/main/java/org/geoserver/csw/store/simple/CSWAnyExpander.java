/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import javax.xml.namespace.QName;
import org.geoserver.csw.util.QNameResolver;
import org.geotools.csw.CSW;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Replaces references to csw:AnyText to a call to the RecordText filter function
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWAnyExpander extends DuplicatingFilterVisitor {

    QNameResolver resolver = new QNameResolver();

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        NamespaceSupport nss = expression.getNamespaceContext();
        String path = expression.getPropertyName();
        if (nss != null) {
            QName name = resolver.parseQName(expression.getPropertyName(), nss);
            String uri = name.getNamespaceURI();
            if (path.endsWith("AnyText")
                    && (uri == null || "".equals(uri) || CSW.NAMESPACE.equals(uri))) {
                return new RecordTextFunction();
            }
        }
        return getFactory(extraData).property(path, nss);
    }
}
