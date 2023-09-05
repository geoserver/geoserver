/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Predicates;
import org.geoserver.function.IsInstanceOf;
import org.geotools.api.filter.And;
import org.geotools.api.filter.BinaryLogicOperator;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.FilterVisitor;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.api.filter.MultiValuedFilter.MatchAction;
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
import org.geotools.api.filter.capability.FilterCapabilities;
import org.geotools.api.filter.expression.Add;
import org.geotools.api.filter.expression.Divide;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.ExpressionVisitor;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.Multiply;
import org.geotools.api.filter.expression.NilExpression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.expression.Subtract;
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
import org.geotools.filter.Capabilities;
import org.geotools.filter.LikeFilterImpl;

/** */
public class FilterToCatalogSQL implements FilterVisitor, ExpressionVisitor {

    public static final FilterCapabilities CAPABILITIES;

    static {
        Capabilities builder = new Capabilities();
        builder.addType(PropertyIsEqualTo.class);
        builder.addType(PropertyIsNotEqualTo.class);
        builder.addType(PropertyIsLike.class);
        builder.addType(PropertyIsNull.class); // whether a property exists at all
        builder.addType(PropertyIsNil.class); // whether the property exists AND it's value is null
        builder.addType(And.class);
        builder.addType(Or.class);
        builder.addName(IsInstanceOf.NAME.getName());

        CAPABILITIES = builder.getContents();
    }

    private final Dialect dialect;

    private final Class<?> queryType;

    private final DbMappings dbMappings;

    private final Map<String, Object> namedParams = new LinkedHashMap<>();

    public FilterToCatalogSQL(Dialect dialect, Class<?> queryType, DbMappings dbMappings) {
        this.dialect = dialect;
        this.queryType = queryType;
        this.dbMappings = dbMappings;
        List<Integer> concreteQueryTypes = dbMappings.getConcreteQueryTypes(queryType);
        namedParams.put("types", concreteQueryTypes);
    }

    /** */
    public Map<String, Object> getNamedParameters() {
        return namedParams;
    }

    private StringBuilder append(Object extraData, String... s) {
        StringBuilder sb = (StringBuilder) extraData;
        for (String p : s) {
            sb.append(p);
        }
        return sb;
    }

    /** @see org.geotools.api.filter.FilterVisitor#visitNullFilter(java.lang.Object) */
    @Override
    public Object visitNullFilter(Object extraData) {
        throw new UnsupportedOperationException("Do not use null as filter");
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.ExcludeFilter,
     *     java.lang.Object)
     */
    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        return dialect.appendComment(append(extraData, "0 = 1"), "EXCLUDE");
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.IncludeFilter,
     *     java.lang.Object)
     */
    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        return dialect.appendComment(append(extraData, "1 = 1"), "INCLUDE");
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsEqualTo,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {

        MatchAction matchAction = filter.getMatchAction();
        boolean matchingCase = filter.isMatchingCase();

        if (!(filter.getExpression1() instanceof Literal)
                && !(filter.getExpression2() instanceof Literal)) {

            // comparing two fields with each other

            PropertyName expression1 = (PropertyName) filter.getExpression1();
            PropertyName expression2 = (PropertyName) filter.getExpression2();

            final String propertyTypesParam1 = propertyTypesParam(expression1);
            final String propertyTypesParam2 = propertyTypesParam(expression2);

            // respect matchCase
            String valueCol1 = matchingCase ? "o1.value" : "UPPER(o1.value)";
            String valueCol2 = matchingCase ? "o2.value" : "UPPER(o2.value)";

            switch (matchAction) {
                    // respect matchaction
                case ALL: // all = another value for the property may not occur
                    append(
                            extraData,
                            "oid NOT IN (SELECT o1.oid FROM object_property o1, object_property o2 WHERE o1.oid = o2.oid ",
                            "AND o1.property_type IN (:",
                            propertyTypesParam1,
                            ") AND o2.property_type IN (:",
                            propertyTypesParam2,
                            ") AND ",
                            valueCol1,
                            " != ",
                            valueCol2,
                            ")");
                    break;
                case ANY: // any = the value for the property must occur at least once
                    append(
                            extraData,
                            "oid IN (SELECT o1.oid FROM object_property o1, object_property o2 WHERE o1.oid = o2.oid ",
                            "AND o1.property_type IN (:",
                            propertyTypesParam1,
                            ") AND o2.property_type IN (:",
                            propertyTypesParam2,
                            ") AND ",
                            valueCol1,
                            " = ",
                            valueCol2,
                            ")");
                    break;
                case ONE: // one = the value for the property must occur exactly once
                    append(
                            extraData,
                            "oid IN (SELECT o1.oid FROM object_property o1, object_property o2 WHERE o1.oid = o2.oid ",
                            "AND o1.property_type IN (:",
                            propertyTypesParam1,
                            ") AND o2.property_type IN (:",
                            propertyTypesParam2,
                            ") AND ",
                            valueCol1,
                            " = ",
                            valueCol2,
                            " GROUP BY (oid) HAVING COUNT(oid) = 1)");
                    break;
                default:
                    throw new IllegalArgumentException("MatchAction: " + matchAction);
            }
        } else {

            if (filter.getExpression1() instanceof IsInstanceOf) {
                return handleInstanceOf((IsInstanceOf) filter.getExpression1(), extraData);
            }

            // comparing a literal with a field

            PropertyName expression1;
            Literal expression2;

            // decide which is the literal
            if (filter.getExpression1() instanceof Literal) {
                expression1 = (PropertyName) filter.getExpression2();
                expression2 = (Literal) filter.getExpression1();

            } else {
                expression1 = (PropertyName) filter.getExpression1();
                expression2 = (Literal) filter.getExpression2();
            }

            final String propertyTypesParam = propertyTypesParam(expression1);

            // respect match case
            String expectedValue = expression2.evaluate(null, String.class);
            if (!matchingCase) {
                expectedValue = expectedValue.toUpperCase();
            }
            String valueParam = newParam("value", expectedValue);
            String valueCol = matchingCase ? "value" : "UPPER(value)";

            switch (matchAction) {
                    // respect match action
                case ALL: // all = another value for the property may not occur
                    append(
                            extraData,
                            "oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:",
                            propertyTypesParam,
                            ") AND ",
                            valueCol,
                            " != :",
                            valueParam,
                            ")");
                    break;
                case ANY: // any = the value for the property must occur at least once
                    append(
                            extraData,
                            "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                            propertyTypesParam,
                            ") AND ",
                            valueCol,
                            " = :",
                            valueParam,
                            ")");
                    break;
                case ONE: // one = the value for the property must occur exactly once
                    append(
                            extraData,
                            "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                            propertyTypesParam,
                            ") AND ",
                            valueCol,
                            " = :",
                            valueParam,
                            " GROUP BY (oid) HAVING COUNT(oid) = 1)");
                    break;
                default:
                    throw new IllegalArgumentException("MatchAction: " + matchAction);
            }
        }
        return dialect.appendComment(extraData, filter);
    }

    private Object handleInstanceOf(IsInstanceOf instanceOf, Object extraData) {
        Expression expression1 = instanceOf.getParameters().get(0);

        Class<?> clazz = expression1.evaluate(null, Class.class);
        Integer typeId = dbMappings.getTypeId(clazz);
        if (typeId == null) {
            return visit(Filter.EXCLUDE, extraData);
        }
        append(extraData, "type_id = ", typeId.toString());
        return dialect.appendComment(extraData, "isInstanceOf ", clazz.getName());
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsLike,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        final PropertyName expression1 = (PropertyName) filter.getExpression();

        final String propertyTypesParam = propertyTypesParam(expression1);

        final String literal = filter.getLiteral();
        final MatchAction matchAction = filter.getMatchAction();
        final char esc = filter.getEscape().charAt(0);
        final char multi = filter.getWildCard().charAt(0);
        final char single = filter.getSingleChar().charAt(0);
        final boolean matchCase = filter.isMatchingCase();

        final String pattern =
                LikeFilterImpl.convertToSQL92(esc, multi, single, matchCase, literal, false);

        // respect match case
        String valueParam = newParam("value", pattern);
        String valueCol = matchCase ? "value" : "UPPER(value)";

        switch (matchAction) {
                // respect match action
            case ALL: // all = another value for the property may not occur
                append(
                        extraData,
                        "oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:",
                        propertyTypesParam,
                        ") AND ",
                        valueCol,
                        " NOT LIKE :",
                        valueParam,
                        ")");
                break;
            case ANY: // any = the value for the property must occur at least once
                append(
                        extraData,
                        "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                        propertyTypesParam,
                        ") AND ",
                        valueCol,
                        " LIKE :",
                        valueParam,
                        ")");
                break;
            case ONE: // one = the value for the property must occur exactly once
                append(
                        extraData,
                        "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                        propertyTypesParam,
                        ") AND ",
                        valueCol,
                        " LIKE :",
                        valueParam,
                        " GROUP BY (oid) HAVING COUNT(oid) = 1)");
                break;
            default:
                throw new IllegalArgumentException("MatchAction: " + matchAction);
        }
        return dialect.appendComment(extraData, filter);
    }

    private String propertyTypesParam(final PropertyName property) {

        final String propertyTypesParam;
        final Set<PropertyType> propertyTypes;

        String propertyName = property.getPropertyName();
        propertyTypes = dbMappings.getPropertyTypes(queryType, propertyName);

        Preconditions.checkState(
                !propertyTypes.isEmpty(),
                "Found no mapping for property '" + property + "' of type " + queryType.getName());

        List<Integer> propTypeIds = new ArrayList<Integer>(propertyTypes.size());
        for (PropertyType pt : propertyTypes) {
            Integer propertyTypeId = pt.getOid();
            propTypeIds.add(propertyTypeId);
        }
        propertyTypesParam = newParam("ptype", propTypeIds);
        return propertyTypesParam;
    }

    /** */
    private String newParam(String paramNamePrefix, Object paramValue) {
        int sufix = 0;
        while (true) {
            String paramName = paramNamePrefix + sufix;
            if (!namedParams.containsKey(paramName)) {
                namedParams.put(paramName, paramValue);
                return paramName;
            }
            sufix++;
        }
    }

    /**
     * @see
     *     org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsNotEqualTo,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        // equivalent to not(propertyisequalto)

        FilterFactory ff = Predicates.factory;
        Not not =
                ff.not(
                        ff.equal(
                                filter.getExpression1(),
                                filter.getExpression2(),
                                filter.isMatchingCase(),
                                filter.getMatchAction()));
        visit(not, extraData);

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.And,
     *     java.lang.Object)
     */
    @Override
    public Object visit(And filter, Object extraData) {
        return visit(filter, "AND", extraData);
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.Or,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Or filter, Object extraData) {
        return visit(filter, "OR", extraData);
    }

    protected Object visit(BinaryLogicOperator filter, String type, Object extraData) {
        StringBuilder sql = (StringBuilder) extraData;
        List<Filter> children = filter.getChildren();
        checkArgument(children.size() > 0);
        sql.append('(');
        dialect.appendIfDebug(sql, "\n    ", "");
        for (Iterator<Filter> it = children.iterator(); it.hasNext(); ) {
            it.next().accept(this, sql);
            if (it.hasNext()) {
                dialect.appendIfDebug(sql, "    ", " ");
                sql.append(type);
                dialect.appendIfDebug(sql, "\n    ", " ");
            }
        }
        return sql.append(')');
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.Id,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Id filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.Not,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Not filter, Object extraData) {
        Filter child = filter.getFilter();
        // these filter types are already enclosed in parentheses
        boolean extraParens =
                !(child instanceof And || child instanceof Or || child instanceof PropertyIsNull);
        append(extraData, "NOT ", extraParens ? "(" : "");
        child.accept(this, extraData);
        return append(extraData, extraParens ? ")" : "");
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsBetween,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsGreaterThan,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsLessThan,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsLessThanOrEqualTo,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsNull,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        final PropertyName propertyName = (PropertyName) filter.getExpression();
        final String propertyTypesParam = propertyTypesParam(propertyName);

        append(
                extraData,
                "(oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                propertyTypesParam,
                ") AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:",
                propertyTypesParam,
                ")))");
        return dialect.appendComment(extraData, filter);
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.PropertyIsNil,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        final PropertyName propertyName = (PropertyName) filter.getExpression();
        final String propertyTypesParam = propertyTypesParam(propertyName);

        append(
                extraData,
                "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                propertyTypesParam,
                ") AND value IS NULL)");
        return dialect.appendComment(extraData, filter);
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.BBOX,
     *     java.lang.Object)
     */
    @Override
    public Object visit(BBOX filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Beyond,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Beyond filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Contains,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Contains filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Crosses,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Crosses filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Disjoint,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Disjoint filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.DWithin,
     *     java.lang.Object)
     */
    @Override
    public Object visit(DWithin filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Equals,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Equals filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Intersects,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Intersects filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Overlaps,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Overlaps filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Touches,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Touches filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.spatial.Within,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Within filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.After,
     *     java.lang.Object)
     */
    @Override
    public Object visit(After after, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.AnyInteracts,
     *     java.lang.Object)
     */
    @Override
    public Object visit(AnyInteracts anyInteracts, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.Before,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Before before, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.Begins,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Begins begins, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.BegunBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(BegunBy begunBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.During,
     *     java.lang.Object)
     */
    @Override
    public Object visit(During during, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.EndedBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(EndedBy endedBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.Ends,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Ends ends, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.Meets,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Meets meets, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.MetBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(MetBy metBy, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.OverlappedBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(OverlappedBy overlappedBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.TContains,
     *     java.lang.Object)
     */
    @Override
    public Object visit(TContains contains, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.TEquals,
     *     java.lang.Object)
     */
    @Override
    public Object visit(TEquals equals, Object extraData) {

        return extraData;
    }

    /**
     * @see org.geotools.api.filter.FilterVisitor#visit(org.geotools.api.filter.temporal.TOverlaps,
     *     java.lang.Object)
     */
    @Override
    public Object visit(TOverlaps contains, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.NilExpression,
     *     java.lang.Object)
     */
    @Override
    public Object visit(NilExpression expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.Add,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Add expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.Divide,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Divide expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.Function,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Function expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.Literal,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Literal expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.Multiply,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Multiply expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.PropertyName,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyName expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.geotools.api.filter.expression.ExpressionVisitor#visit(org.geotools.api.filter.expression.Subtract,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Subtract expression, Object extraData) {

        throw new UnsupportedOperationException();
    }
}
