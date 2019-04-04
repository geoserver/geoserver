/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This is to set namespace context to handle complex attributes in the bbox filter.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class BBOXNamespaceSettingVisitor extends DuplicatingFilterVisitor {

    private NamespaceSupport nsContext;

    public BBOXNamespaceSettingVisitor(NamespaceSupport ns) {
        nsContext = ns;
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        String propertyName = null;
        if (filter.getExpression1() instanceof PropertyName) {
            propertyName = ((PropertyName) filter.getExpression1()).getPropertyName();
        } else if (filter.getExpression2() instanceof PropertyName) {
            propertyName = ((PropertyName) filter.getExpression2()).getPropertyName();
        }

        if (propertyName != null) {
            PropertyName propertyAtt = ff.property(propertyName, nsContext);
            filter = ff.bbox(propertyAtt, filter.getBounds());
        }
        return filter;
    }
}
