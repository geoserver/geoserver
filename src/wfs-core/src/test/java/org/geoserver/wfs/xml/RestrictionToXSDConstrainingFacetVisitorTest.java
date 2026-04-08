package org.geoserver.wfs.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.eclipse.xsd.XSDConstrainingFacet;
import org.eclipse.xsd.impl.XSDFactoryImpl;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Test;

public class RestrictionToXSDConstrainingFacetVisitorTest {

    private final RestrictionToXSDConstrainingFacetVisitor facetVisitor =
            new RestrictionToXSDConstrainingFacetVisitor(new XSDFactoryImpl());
    private final FilterFactory filterFactory = new FilterFactoryImpl();

    @Test
    public void testIsEqualToVisit() {

        Expression property = filterFactory.property("prop");
        Expression literal = filterFactory.literal(1);
        PropertyIsEqualTo isEqualTo = filterFactory.equals(property, literal);

        Or or = filterFactory.or(List.of(isEqualTo, isEqualTo, isEqualTo, isEqualTo, isEqualTo));

        List<XSDConstrainingFacet> visit = facetVisitor.visit(or, null);

        assertEquals(or.getChildren().size(), visit.size());
        visit.forEach(facet -> assertEquals("1", facet.getLexicalValue()));
    }

    @Test
    public void testIsBetweenVisit() {

        Expression minLiteral = filterFactory.literal(0);
        Expression maxLiteral = filterFactory.literal(1);
        PropertyIsBetween isBetween = filterFactory.between(filterFactory.literal(0.5), minLiteral, maxLiteral);

        List<XSDConstrainingFacet> visit = facetVisitor.visit(isBetween, null);

        assertEquals(2, visit.size());

        XSDConstrainingFacet min = visit.get(0);
        assertEquals("0", min.getLexicalValue());

        XSDConstrainingFacet max = visit.get(1);
        assertEquals("1", max.getLexicalValue());
    }
}
