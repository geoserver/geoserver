/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.eclipse.xsd.XSDConstrainingFacet;
import org.eclipse.xsd.XSDEnumerationFacet;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDMaxInclusiveFacet;
import org.eclipse.xsd.XSDMinInclusiveFacet;
import org.geotools.api.filter.And;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterVisitor;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNil;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Beyond;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Crosses;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Equals;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Overlaps;
import org.geotools.api.filter.spatial.Touches;
import org.geotools.api.filter.spatial.Within;
import org.geotools.api.filter.temporal.After;
import org.geotools.api.filter.temporal.AnyInteracts;
import org.geotools.api.filter.temporal.Before;
import org.geotools.api.filter.temporal.Begins;
import org.geotools.api.filter.temporal.BegunBy;
import org.geotools.api.filter.temporal.During;
import org.geotools.api.filter.temporal.EndedBy;
import org.geotools.api.filter.temporal.Ends;
import org.geotools.api.filter.temporal.Meets;
import org.geotools.api.filter.temporal.MetBy;
import org.geotools.api.filter.temporal.OverlappedBy;
import org.geotools.api.filter.temporal.TContains;
import org.geotools.api.filter.temporal.TEquals;
import org.geotools.api.filter.temporal.TOverlaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RestrictionToXSDConstrainingFacetVisitor implements FilterVisitor {
    private final XSDFactory factory;

    RestrictionToXSDConstrainingFacetVisitor(XSDFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object visitNullFilter(Object extraData) {
        return null;
    }

    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(And filter, Object extraData) {
        return visitChildren(filter.getChildren(), extraData);
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        return visitChildren(filter.getChildren(), extraData);
    }

    @Override
    public Object visit(Not filter, Object extraData) {
        return visitChildren(List.of(filter.getFilter()), extraData);
    }

    private Object visitChildren(List<Filter> children, Object extraData) {
        children.stream().map(c -> c.accept(this, extraData)).forEach(facetsLists -> ((ArrayList<XSDConstrainingFacet>)
                        extraData)
                .addAll((Collection<? extends XSDConstrainingFacet>) facetsLists));
        return extraData;
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        return null;
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

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        XSDMinInclusiveFacet minFacet = factory.createXSDMinInclusiveFacet();
        minFacet.setValue(filter.getExpression2().toString());
        minFacet.setLexicalValue(filter.getExpression2().toString());

        ((ArrayList<XSDConstrainingFacet>) extraData).add(minFacet);
        return extraData;
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        XSDMaxInclusiveFacet maxFacet = factory.createXSDMaxInclusiveFacet();
        maxFacet.setValue(filter.getExpression2().toString());
        maxFacet.setLexicalValue(filter.getExpression2().toString());

        ((ArrayList<XSDConstrainingFacet>) extraData).add(maxFacet);
        return extraData;
    }

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Contains filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Crosses filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Disjoint filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Equals filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Intersects filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Overlaps filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Touches filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Within filter, Object extraData) {
        return null;
    }

    @Override
    public Object visit(After after, Object extraData) {
        return null;
    }

    @Override
    public Object visit(AnyInteracts anyInteracts, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Before before, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Begins begins, Object extraData) {
        return null;
    }

    @Override
    public Object visit(BegunBy begunBy, Object extraData) {
        return null;
    }

    @Override
    public Object visit(During during, Object extraData) {
        return null;
    }

    @Override
    public Object visit(EndedBy endedBy, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Ends ends, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Meets meets, Object extraData) {
        return null;
    }

    @Override
    public Object visit(MetBy metBy, Object extraData) {
        return null;
    }

    @Override
    public Object visit(OverlappedBy overlappedBy, Object extraData) {
        return null;
    }

    @Override
    public Object visit(TContains contains, Object extraData) {
        return null;
    }

    @Override
    public Object visit(TEquals equals, Object extraData) {
        return null;
    }

    @Override
    public Object visit(TOverlaps contains, Object extraData) {
        return null;
    }
}
