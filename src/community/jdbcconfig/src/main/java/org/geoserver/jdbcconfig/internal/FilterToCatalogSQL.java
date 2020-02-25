/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Predicates;
import org.geoserver.function.IsInstanceOf;
import org.geotools.filter.Capabilities;
import org.geotools.filter.LikeFilterImpl;
import org.opengis.filter.And;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.MultiValuedFilter.MatchAction;
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
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
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

    private final Class<?> queryType;

    private final DbMappings dbMappings;

    private Map<String, Object> namedParams;

    public FilterToCatalogSQL(Class<?> queryType, DbMappings dbMappings) {
        this.queryType = queryType;
        this.dbMappings = dbMappings;
        namedParams = Maps.newHashMap();
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

    /** @see org.opengis.filter.FilterVisitor#visitNullFilter(java.lang.Object) */
    @Override
    public Object visitNullFilter(Object extraData) {
        throw new UnsupportedOperationException("Do not use null as filter");
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.ExcludeFilter,
     *     java.lang.Object)
     */
    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        append(extraData, "(1=0) /* EXCLUDE */\n");
        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.IncludeFilter,
     *     java.lang.Object)
     */
    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        append(extraData, "(1=1) /* INCLUDE */\n");
        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsEqualTo,
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

            StringBuilder builder;

            switch (matchAction) {
                    // respect matchaction
                case ALL: // all = another value for the property may not occur
                    builder =
                            append(
                                    extraData,
                                    "oid NOT IN (SELECT o1.oid FROM object_property o1, object_property o2 WHERE(o1.oid=o2.oid)  ",
                                    "AND o1.property_type IN (:",
                                    propertyTypesParam1,
                                    ") ",
                                    "AND o2.property_type IN (:",
                                    propertyTypesParam2,
                                    ") ",
                                    "AND ",
                                    valueCol1,
                                    " != ",
                                    valueCol2,
                                    " ) /* ",
                                    filter.toString(),
                                    " */ \n");
                    break;
                case ANY: // any = the value for the property must occur at least once
                    builder =
                            append(
                                    extraData,
                                    "oid IN (SELECT o1.oid FROM object_property o1, object_property o2 WHERE(o1.oid=o2.oid)  ",
                                    "AND o1.property_type IN (:",
                                    propertyTypesParam1,
                                    ") ",
                                    "AND o2.property_type IN (:",
                                    propertyTypesParam2,
                                    ") ",
                                    "AND ",
                                    valueCol1,
                                    " = ",
                                    valueCol2,
                                    " ) /* ",
                                    filter.toString(),
                                    " */ \n");
                    break;
                case ONE: // one = the value for the property must occur exactly once
                    builder =
                            append(
                                    extraData,
                                    "oid IN (SELECT o1.oid FROM object_property o1, object_property o2 WHERE(o1.oid=o2.oid) ",
                                    "AND o1.property_type IN (:",
                                    propertyTypesParam1,
                                    ") ",
                                    "AND o2.property_type IN (:",
                                    propertyTypesParam2,
                                    ") ",
                                    "AND ",
                                    valueCol1,
                                    " = ",
                                    valueCol2,
                                    " GROUP BY (oid) HAVING COUNT(oid)=1) /* ",
                                    filter.toString(),
                                    "/* ",
                                    filter.toString(),
                                    " */ \n");
                    break;
                default:
                    throw new IllegalArgumentException("MatchAction: " + matchAction);
            }

            return builder;

        } else {

            if (filter.getExpression1() instanceof IsInstanceOf) {
                StringBuilder builder =
                        append(extraData, handleInstanceOf((IsInstanceOf) filter.getExpression1()));
                return builder;
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

            StringBuilder builder;
            String valueCol = matchingCase ? "value" : "UPPER(value)";

            switch (matchAction) {
                    // respect match action
                case ALL: // all = another value for the property may not occur
                    builder =
                            append(
                                    extraData,
                                    "oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:",
                                    propertyTypesParam,
                                    ") AND ",
                                    valueCol,
                                    " != :",
                                    valueParam,
                                    ") /* ",
                                    filter.toString(),
                                    " */ \n");
                    break;
                case ANY: // any = the value for the property must occur at least once
                    builder =
                            append(
                                    extraData,
                                    "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                                    propertyTypesParam,
                                    ") AND ",
                                    valueCol,
                                    " = :",
                                    valueParam,
                                    ") /* ",
                                    filter.toString(),
                                    " */ \n");
                    break;
                case ONE: // one = the value for the property must occur exactly once
                    builder =
                            append(
                                    extraData,
                                    "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                                    propertyTypesParam,
                                    ") AND ",
                                    valueCol,
                                    " = :",
                                    valueParam,
                                    " GROUP BY (oid) HAVING COUNT(oid)=1) /* ",
                                    filter.toString(),
                                    " */ \n");
                    break;
                default:
                    throw new IllegalArgumentException("MatchAction: " + matchAction);
            }

            return builder;
        }
    }

    private String handleInstanceOf(IsInstanceOf instanceOf) {
        Expression expression1 = instanceOf.getParameters().get(0);

        Class clazz = expression1.evaluate(null, Class.class);

        if (clazz == null || dbMappings.getTypeId(clazz) == null) {
            return "(1=0) /* EXCLUDE */\n";
        }

        Integer typeId = dbMappings.getTypeId(clazz);

        return "type_id = " + typeId + "/* isInstanceOf " + clazz.toString() + " */ \n";
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsLike,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        final PropertyName expression1 = (PropertyName) filter.getExpression();
        // TODO: check for indexed property name

        final String propertyTypesParam = propertyTypesParam(expression1);

        final String literal = filter.getLiteral();
        final MatchAction matchAction = filter.getMatchAction();
        final char esc = filter.getEscape().charAt(0);
        final char multi = filter.getWildCard().charAt(0);
        final char single = filter.getSingleChar().charAt(0);
        final boolean matchCase = filter.isMatchingCase();

        final String pattern =
                LikeFilterImpl.convertToSQL92(esc, multi, single, matchCase, literal);

        // respect match case
        String valueCol = matchCase ? "value" : "UPPER(value)";

        StringBuilder builder;

        switch (matchAction) {
                // respect match action
            case ALL: // all = another value for the property may not occur
                builder =
                        append(
                                extraData,
                                "oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:",
                                propertyTypesParam,
                                ") AND NOT(",
                                valueCol,
                                " LIKE '",
                                pattern,
                                "')) /* ",
                                filter.toString(),
                                " */ \n");
                break;
            case ANY: // any = the value for the property must occur at least once
                builder =
                        append(
                                extraData,
                                "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                                propertyTypesParam,
                                ") AND ",
                                valueCol,
                                " LIKE '",
                                pattern,
                                "') /* ",
                                filter.toString(),
                                " */ \n");
                break;
            case ONE: // one = the value for the property must occur exactly once
                builder =
                        append(
                                extraData,
                                "oid IN (SELECT oid FROM object_property WHERE property_type IN (:",
                                propertyTypesParam,
                                ") AND ",
                                valueCol,
                                " LIKE '",
                                pattern,
                                "' ",
                                "GROUP BY (oid) HAVING COUNT(oid)=1 ) /* ",
                                filter.toString(),
                                " */ \n");
                break;
            default:
                throw new IllegalArgumentException("MatchAction: " + matchAction);
        }

        return builder;
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
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsNotEqualTo,
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

    /** @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.And, java.lang.Object) */
    @Override
    public Object visit(And filter, Object extraData) {
        StringBuilder sql = (StringBuilder) extraData;

        List<Filter> children = filter.getChildren();
        checkArgument(children.size() > 0);
        sql.append("(\n\t");

        for (Iterator<Filter> it = children.iterator(); it.hasNext(); ) {
            Filter child = it.next();
            sql = (StringBuilder) child.accept(this, sql);
            if (it.hasNext()) {
                sql = append(extraData, "\tAND\n\t");
            }
        }
        sql.append(")");
        return sql;
    }

    /** @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.Or, java.lang.Object) */
    @Override
    public Object visit(Or filter, Object extraData) {
        StringBuilder sql = (StringBuilder) extraData;

        List<Filter> children = filter.getChildren();
        checkArgument(children.size() > 0);
        sql.append("(");
        for (Iterator<Filter> it = children.iterator(); it.hasNext(); ) {
            Filter child = it.next();
            sql = (StringBuilder) child.accept(this, sql);
            if (it.hasNext()) {
                sql = append(extraData, "\tOR\n\t");
            }
        }
        sql.append(")");
        return sql;
    }

    /** @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.Id, java.lang.Object) */
    @Override
    public Object visit(Id filter, Object extraData) {

        return extraData;
    }

    /** @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.Not, java.lang.Object) */
    @Override
    public Object visit(Not filter, Object extraData) {

        return filter.getFilter().accept(this, append(extraData, " NOT "));
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsBetween,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsGreaterThan,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsGreaterThanOrEqualTo,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsLessThan,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsLessThanOrEqualTo,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsNull,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        final PropertyName propertyName = (PropertyName) filter.getExpression();
        final String propertyTypesParam = propertyTypesParam(propertyName);

        StringBuilder builder =
                append(
                        extraData,
                        "(oid IN (select oid from object_property where property_type in (:",
                        propertyTypesParam,
                        ") and value IS NULL) OR oid NOT  in (select oid from object_property where property_type in (:"
                                + propertyTypesParam
                                + "))) /* ",
                        filter.toString(),
                        " */ \n");
        return builder;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.PropertyIsNil,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        final PropertyName propertyName = (PropertyName) filter.getExpression();
        final String propertyTypesParam = propertyTypesParam(propertyName);

        StringBuilder builder =
                append(
                        extraData,
                        "oid IN (select oid from object_property where property_type in (:",
                        propertyTypesParam,
                        ") and value IS NULL) /* ",
                        filter.toString(),
                        " */ \n");
        return builder;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.BBOX,
     *     java.lang.Object)
     */
    @Override
    public Object visit(BBOX filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Beyond,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Beyond filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Contains,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Contains filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Crosses,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Crosses filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Disjoint,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Disjoint filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.DWithin,
     *     java.lang.Object)
     */
    @Override
    public Object visit(DWithin filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Equals,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Equals filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Intersects,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Intersects filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Overlaps,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Overlaps filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Touches,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Touches filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.spatial.Within,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Within filter, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.After,
     *     java.lang.Object)
     */
    @Override
    public Object visit(After after, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.AnyInteracts,
     *     java.lang.Object)
     */
    @Override
    public Object visit(AnyInteracts anyInteracts, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Before,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Before before, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Begins,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Begins begins, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.BegunBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(BegunBy begunBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.During,
     *     java.lang.Object)
     */
    @Override
    public Object visit(During during, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.EndedBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(EndedBy endedBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Ends,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Ends ends, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.Meets,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Meets meets, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.MetBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(MetBy metBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.OverlappedBy,
     *     java.lang.Object)
     */
    @Override
    public Object visit(OverlappedBy overlappedBy, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.TContains,
     *     java.lang.Object)
     */
    @Override
    public Object visit(TContains contains, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.TEquals,
     *     java.lang.Object)
     */
    @Override
    public Object visit(TEquals equals, Object extraData) {

        return extraData;
    }

    /**
     * @see org.opengis.filter.FilterVisitor#visit(org.opengis.filter.temporal.TOverlaps,
     *     java.lang.Object)
     */
    @Override
    public Object visit(TOverlaps contains, Object extraData) {

        return extraData;
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.NilExpression,
     *     java.lang.Object)
     */
    @Override
    public Object visit(NilExpression expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Add,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Add expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Divide,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Divide expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Function,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Function expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Literal,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Literal expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Multiply,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Multiply expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.PropertyName,
     *     java.lang.Object)
     */
    @Override
    public Object visit(PropertyName expression, Object extraData) {

        throw new UnsupportedOperationException();
    }

    /**
     * @see
     *     org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Subtract,
     *     java.lang.Object)
     */
    @Override
    public Object visit(Subtract expression, Object extraData) {

        throw new UnsupportedOperationException();
    }
}
