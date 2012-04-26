package org.geoserver.bxml.filter_1_1;

import static org.geotools.filter.v1_1.OGC.Add;
import static org.geotools.filter.v1_1.OGC.And;
import static org.geotools.filter.v1_1.OGC.BBOX;
import static org.geotools.filter.v1_1.OGC.Beyond;
import static org.geotools.filter.v1_1.OGC.Contains;
import static org.geotools.filter.v1_1.OGC.Crosses;
import static org.geotools.filter.v1_1.OGC.DWithin;
import static org.geotools.filter.v1_1.OGC.Disjoint;
import static org.geotools.filter.v1_1.OGC.Div;
import static org.geotools.filter.v1_1.OGC.Equals;
import static org.geotools.filter.v1_1.OGC.FeatureId;
import static org.geotools.filter.v1_1.OGC.Function;
import static org.geotools.filter.v1_1.OGC.Intersects;
import static org.geotools.filter.v1_1.OGC.Literal;
import static org.geotools.filter.v1_1.OGC.Mul;
import static org.geotools.filter.v1_1.OGC.Not;
import static org.geotools.filter.v1_1.OGC.Or;
import static org.geotools.filter.v1_1.OGC.Overlaps;
import static org.geotools.filter.v1_1.OGC.PropertyIsBetween;
import static org.geotools.filter.v1_1.OGC.PropertyIsEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsGreaterThan;
import static org.geotools.filter.v1_1.OGC.PropertyIsGreaterThanOrEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsLessThan;
import static org.geotools.filter.v1_1.OGC.PropertyIsLessThanOrEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsLike;
import static org.geotools.filter.v1_1.OGC.PropertyIsNotEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsNull;
import static org.geotools.filter.v1_1.OGC.PropertyName;
import static org.geotools.filter.v1_1.OGC.Sub;
import static org.geotools.filter.v1_1.OGC.Touches;
import static org.geotools.filter.v1_1.OGC.Within;

import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.AbstractEncoder;
import org.geoserver.bxml.gml_3_1.AbstractGMLEncoder;
import org.geoserver.bxml.gml_3_1.GeometryEncoder;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml3.GML;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.BinaryExpression;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;

import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Encodes a {@link Filter} to XML in OGC FES 1.1 format.
 * <p>
 * NOTE: The following methods are still unsupported as they come from FES 2.0:
 * <ul>
 * <li> {@link #visit(After, Object)}
 * <li> {@link #visit(AnyInteracts, Object)}
 * <li> {@link #visit(Before, Object)}
 * <li> {@link #visit(Begins, Object)}
 * <li> {@link #visit(BegunBy, Object)}
 * <li> {@link #visit(EndedBy, Object)}
 * <li> {@link #visit(Ends, Object)}
 * <li> {@link #visit(TContains, Object)}
 * <li> {@link #visit(TEquals, Object)}
 * <li> {@link #visit(TOverlaps, Object)}
 * <li> {@link #visit(Meets, Object)}
 * <li> {@link #visit(MetBy, Object)}
 * </ul>
 * 
 */
public class FilterEncoder extends AbstractEncoder<Filter> {

    @Override
    public void encode(final Filter filter, final BxmlStreamWriter w) throws IOException {
        FilterVisitor filterEncoderVisitor = new FilterEncoderVisitor(w);
        try {
            filter.accept(filterEncoderVisitor, null);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private class FilterEncoderVisitor implements FilterVisitor, ExpressionVisitor {
        private BxmlStreamWriter w;

        private AbstractGMLEncoder geometryEncoder;

        private AbstractGMLEncoder getGeometryEncoder() {
            if (geometryEncoder == null) {
                geometryEncoder = new GeometryEncoder();
            }
            return geometryEncoder;
        }

        public FilterEncoderVisitor(BxmlStreamWriter w) {
            this.w = w;
        }

        public Object visitNullFilter(Object extraData) {
            throw new IllegalArgumentException(
                    "NULLFilter is not supported. Clean up the filter before encoding");
        }

        public Object visit(ExcludeFilter filter, Object extraData) {
            throw new IllegalArgumentException(
                    "ExcludeFilter is not supported. Clean up the filter before encoding");
        }

        public Object visit(IncludeFilter filter, Object extraData) {
            throw new IllegalArgumentException(
                    "IncludeFilter is not supported. Clean up the filter before encoding");
        }

        public Object visit(And filter, Object extraData) {
            startElement(w, And);
            {
                for (Filter anded : filter.getChildren()) {
                    anded.accept(this, extraData);
                }
            }
            endElement(w);
            return extraData;
        }

        public Object visit(Id filter, Object extraData) {
            Set<Identifier> identifiers = filter.getIdentifiers();
            for (Identifier i : identifiers) {
                element(w, FeatureId, true, null, "fid", i.getID().toString());
            }
            return extraData;
        }

        public Object visit(Not filter, Object extraData) {
            startElement(w, Not);
            {
                Filter negated = filter.getFilter();
                negated.accept(this, extraData);
                endElement(w);
            }
            return extraData;
        }

        public Object visit(Or filter, Object extraData) {
            startElement(w, Or);
            {
                for (Filter ored : filter.getChildren()) {
                    ored.accept(this, extraData);
                }
            }
            endElement(w);
            return extraData;
        }

        public Object visit(PropertyIsBetween filter, Object extraData) {
            startElement(w, PropertyIsBetween);
            {
                Expression expression = filter.getExpression();
                Expression lowerBoundary = filter.getLowerBoundary();
                Expression upperBoundary = filter.getUpperBoundary();

                expression.accept(this, extraData);
                startElement(w, new QName(OGC.NAMESPACE, "LowerBoundary"));
                {
                    lowerBoundary.accept(this, extraData);
                }
                endElement(w);

                startElement(w, new QName(OGC.NAMESPACE, "UpperBoundary"));
                {
                    upperBoundary.accept(this, extraData);
                }
                endElement(w);
            }
            endElement(w);
            return extraData;
        }

        private Object visit(BinaryComparisonOperator filter, QName name, Object extraData) {
            filter.isMatchingCase();
            startElement(w, name);
            attributes(w, "matchCase", String.valueOf(filter.isMatchingCase()));
            {
                Expression expression1 = filter.getExpression1();
                Expression expression2 = filter.getExpression2();
                expression1.accept(this, extraData);
                expression2.accept(this, extraData);
            }
            endElement(w);
            return extraData;
        }

        public Object visit(PropertyIsEqualTo filter, Object extraData) {
            return visit(filter, PropertyIsEqualTo, extraData);
        }

        public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
            return visit(filter, PropertyIsNotEqualTo, extraData);
        }

        public Object visit(PropertyIsGreaterThan filter, Object extraData) {
            return visit(filter, PropertyIsGreaterThan, extraData);
        }

        public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
            return visit(filter, PropertyIsGreaterThanOrEqualTo, extraData);
        }

        public Object visit(PropertyIsLessThan filter, Object extraData) {
            return visit(filter, PropertyIsLessThan, extraData);
        }

        public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
            return visit(filter, PropertyIsLessThanOrEqualTo, extraData);
        }

        public Object visit(PropertyIsLike filter, Object extraData) {
            startElement(w, PropertyIsLike);
            attributes(w, "matchCase", String.valueOf(filter.isMatchingCase()), "singleChar",
                    filter.getSingleChar(), "escaleChar", filter.getEscape(), "wildCard",
                    filter.getWildCard());
            {
                Expression expression = filter.getExpression();
                expression.accept(this, extraData);

                String literal = filter.getLiteral();
                element(w, Literal, true, literal);
            }
            endElement(w);
            return extraData;
        }

        public Object visit(PropertyIsNull filter, Object extraData) {
            startElement(w, PropertyIsNull);
            {
                Expression expression = filter.getExpression();
                expression.accept(this, extraData);
            }
            endElement(w);
            return extraData;
        }

        private Object visit(DistanceBufferOperator filter, QName name, Object extraData) {
            startElement(w, name);
            {
                Expression expression1 = filter.getExpression1();
                Expression expression2 = filter.getExpression2();
                expression1.accept(this, extraData);
                expression2.accept(this, extraData);

                element(w, new QName(OGC.NAMESPACE, "Distance"), true,
                        String.valueOf(filter.getDistance()), "units", filter.getDistanceUnits());

            }
            endElement(w);
            return extraData;
        }

        public Object visit(Beyond filter, Object extraData) {
            return visit((DistanceBufferOperator) filter, Beyond, extraData);
        }

        public Object visit(DWithin filter, Object extraData) {
            return visit((DistanceBufferOperator) filter, DWithin, extraData);
        }

        private Object visit(BinarySpatialOperator filter, QName name, Object extraData,
                String... attributes) {
            startElement(w, name);
            attributes(w, attributes);
            {
                Expression expression1 = filter.getExpression1();
                Expression expression2 = filter.getExpression2();
                expression1.accept(this, extraData);
                expression2.accept(this, extraData);
            }
            endElement(w);
            return extraData;
        }

        public Object visit(BBOX filter, Object extraData) {
            return visit(filter, BBOX, extraData);
        }

        public Object visit(Contains filter, Object extraData) {
            return visit(filter, Contains, extraData);
        }

        public Object visit(Crosses filter, Object extraData) {
            return visit(filter, Crosses, extraData);
        }

        public Object visit(Disjoint filter, Object extraData) {
            return visit(filter, Disjoint, extraData);
        }

        public Object visit(Equals filter, Object extraData) {
            return visit(filter, Equals, extraData);
        }

        public Object visit(Intersects filter, Object extraData) {
            return visit(filter, Intersects, extraData);
        }

        public Object visit(Overlaps filter, Object extraData) {
            return visit(filter, Overlaps, extraData);
        }

        public Object visit(Touches filter, Object extraData) {
            return visit(filter, Touches, extraData);
        }

        public Object visit(Within filter, Object extraData) {
            return visit(filter, Within, extraData);
        }

        /**
         * Implements {@link
         * org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsNil,
         * java.lang.Object}
         */
        @Override
        public Object visit(PropertyIsNil filter, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.After, java.lang.Object)}
         */
        public Object visit(After after, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.AnyInteracts, java.lang.Object)}
         */
        public Object visit(AnyInteracts anyInteracts, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Before, java.lang.Object)}
         */
        public Object visit(Before before, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Begins, java.lang.Object)}
         */
        public Object visit(Begins begins, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.BegunBy, java.lang.Object)}
         */
        public Object visit(BegunBy begunBy, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.During, java.lang.Object)}
         */
        public Object visit(During during, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.EndedBy, java.lang.Object)}
         */
        public Object visit(EndedBy endedBy, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Ends, java.lang.Object)}
         */
        public Object visit(Ends ends, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Meets, java.lang.Object)}
         */
        public Object visit(Meets meets, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.MetBy, java.lang.Object)}
         */
        public Object visit(MetBy metBy, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.OverlappedBy, java.lang.Object)}
         */
        public Object visit(OverlappedBy overlappedBy, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.TContains, java.lang.Object)}
         */
        public Object visit(TContains contains, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.TEquals, java.lang.Object)}
         */
        public Object visit(TEquals equals, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        /**
         * Implements
         * {@link org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.TOverlaps, java.lang.Object)}
         */
        public Object visit(TOverlaps contains, Object extraData) {
            throw new UnsupportedOperationException("FES 2.0 encoding not yet supported");
        }

        // ///////////// Expressions /////////////////

        public Object visit(NilExpression expression, Object extraData) {
            element(w, Literal, true, (String) null);
            return extraData;
        }

        private Object visit(BinaryExpression expression, QName name, Object extraData) {
            startElement(w, name);
            {
                Expression expression1 = expression.getExpression1();
                Expression expression2 = expression.getExpression2();
                expression1.accept(this, extraData);
                expression2.accept(this, extraData);
            }
            endElement(w);
            return extraData;
        }

        public Object visit(Add expression, Object extraData) {
            return visit(expression, Add, extraData);
        }

        public Object visit(Divide expression, Object extraData) {
            return visit(expression, Div, extraData);
        }

        public Object visit(Multiply expression, Object extraData) {
            return visit(expression, Mul, extraData);
        }

        public Object visit(Subtract expression, Object extraData) {
            return visit(expression, Sub, extraData);
        }

        public Object visit(Function expression, Object extraData) {
            startElement(w, Function);
            attributes(w, "name", expression.getName());
            {
                for (Expression param : expression.getParameters()) {
                    param.accept(this, extraData);
                }
            }
            endElement(w);
            return extraData;
        }

        public Object visit(PropertyName expression, Object extraData) {
            element(w, PropertyName, true, expression.getPropertyName());
            return extraData;
        }

        public Object visit(Literal expression, Object extraData) {
            final Object value = expression.getValue();
            if (value == null) {
                element(w, GML.Null, true, (String) null);
            } else if (value instanceof Geometry) {
                final Geometry geometry = (Geometry) value;
                try {
                    getGeometryEncoder().encode(geometry, w);
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            } else {
                String evaluated = expression.evaluate(null, String.class);
                element(w, Literal, true, evaluated);
            }
            return extraData;
        }
    }

}
