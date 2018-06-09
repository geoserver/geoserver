/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Expands the paths referring to SimpleLiteral instances so that they contain the dc:value ending,
 * and moves the filter against the bbox to the internal geometry
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CRSRecordProjectyPathAdapter extends DuplicatingFilterVisitor {
    CSWPropertyPathExtender extender = new CSWPropertyPathExtender();

    public CRSRecordProjectyPathAdapter(NamespaceSupport defaultNss) {
        super();
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        FilterFactory2 filterFactory = getFactory(extraData);
        NamespaceSupport nss = expression.getNamespaceContext();

        return extender.extendProperty(expression, filterFactory, nss);
    }
}
