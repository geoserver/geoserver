/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import org.geotools.data.complex.util.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

/**
 * Filter Visitor that translates queryable names to x-paths according to the ISO MetaData Profile
 * Standard
 *
 * @author Niels Charlier
 */
public class MDQueryableFilterVisitor extends DuplicatingFilterVisitor {

    protected static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    @Override
    public Object visit(PropertyName expression, Object extraData) {

        return property(expression);
    }

    /**
     * Helper method to translate propertyname that possibly contains queryable name to xml x-path
     *
     * @param expression property name
     */
    public static PropertyName property(PropertyName expression) {

        XPathUtil.StepList steps =
                XPathUtil.steps(
                        MetaDataDescriptor.METADATA_DESCRIPTOR,
                        expression.getPropertyName(),
                        MetaDataDescriptor.NAMESPACES);

        if (steps.size() == 1 && steps.get(0).getName().getNamespaceURI() == null
                || steps.get(0)
                        .getName()
                        .getNamespaceURI()
                        .equals(MetaDataDescriptor.NAMESPACE_APISO)) {
            PropertyName fullPath =
                    MetaDataDescriptor.QUERYABLE_MAPPING.get(steps.get(0).getName().getLocalPart());
            if (fullPath != null) {
                return fullPath;
            }
        }

        return expression;
    }
}
