/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.xsd.XSDConstrainingFacet;
import org.eclipse.xsd.XSDEnumerationFacet;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDMaxInclusiveFacet;
import org.eclipse.xsd.XSDMinInclusiveFacet;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.filter.visitor.AbstractFilterVisitor;

/**
 * This visitor is dedicated to translate known {@link Filter}s generated from {@link AttributeTypeInfo} restrictions,
 * into {@link XSDConstrainingFacet}s to be exposed in a WFS DescribeFeatureType response.
 *
 * <p>The filters that this visitor will accept are:
 *
 * <ul>
 *   <li>{@link PropertyIsEqualTo}, representing each element of {@link AttributeTypeInfo#getOptions() options
 *       restriction}
 *   <li>{@link Or}, to chain options equality checks
 *   <li>{@link PropertyIsBetween}, representing the {@link AttributeTypeInfo#getRange() range restriction}
 * </ul>
 */
@SuppressWarnings("unchecked")
public class RestrictionToXSDConstrainingFacetVisitor extends AbstractFilterVisitor {
    private final XSDFactory factory;

    RestrictionToXSDConstrainingFacetVisitor(XSDFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        return visitChildren(filter.getChildren(), extraData);
    }

    private Object visitChildren(List<Filter> children, Object extraData) {
        children.stream().map(c -> c.accept(this, extraData)).forEach(facetsLists -> ((ArrayList<XSDConstrainingFacet>)
                        extraData)
                .addAll((Collection<? extends XSDConstrainingFacet>) facetsLists));
        return extraData;
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        XSDMinInclusiveFacet minFacet = factory.createXSDMinInclusiveFacet();
        minFacet.setValue(filter.getLowerBoundary().toString());
        minFacet.setLexicalValue(filter.getLowerBoundary().toString());
        XSDMaxInclusiveFacet maxFacet = factory.createXSDMaxInclusiveFacet();
        maxFacet.setValue(filter.getUpperBoundary().toString());
        maxFacet.setLexicalValue(filter.getUpperBoundary().toString());

        ((ArrayList<XSDConstrainingFacet>) extraData).addAll(List.of(minFacet, maxFacet));

        return extraData;
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        XSDEnumerationFacet enumerationFacet = factory.createXSDEnumerationFacet();
        enumerationFacet.setLexicalValue(filter.getExpression2().toString());

        ((ArrayList<XSDConstrainingFacet>) extraData).add(enumerationFacet);
        return extraData;
    }
}
