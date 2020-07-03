/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Converters;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Static factory method utility to build well known types of {@link Filter} instances.
 *
 * <p>Although {@code Catalog} client code is allowed to use any (well behaving) {@code Filter}, the
 * factory methods in this utility class construct predicate instances of well-known types, in order
 * to aid catalog backend implementations in transforming the predicates to their native query
 * languages.
 *
 * <p>The factory methods in this utility also allow for a more compact code by using static
 * imports, so that, for example:
 *
 * <pre>
 * <code>
 * FilterFactory ff = CommonFactoryFinder.getFilterFactory();
 * Filter f1 = ff.equals(ff.propertyName('name'), ff.literal('roads'));
 * Filter f2 = ff.equals(ff.propertyName('name'), ff.literal('streams'));
 * Filter f3 = ff.equals(ff.propertyName('enabled'), ff.literal(Boolean.TRUE));
 * Filter filter = ff.and(ff.or(f1, f2), f3);
 * </code>
 * </pre>
 *
 * becomes:
 *
 * <pre>
 * <code>
 * Filter filter = and(
 *              or(equal('name', 'roads'),equal('name', 'streams')),
 *              equal('enabled', Boolean.TRUE));
 * </code>
 * </pre>
 */
@ParametersAreNonnullByDefault
public class Predicates {

    public static final FilterFactory factory = CommonFactoryFinder.getFilterFactory();

    public static final PropertyName ANY_TEXT = factory.property("AnyText");

    private Predicates() {
        //
    }

    /** @return the "no-filter" predicate. */
    public static Filter acceptAll() {
        return Filter.INCLUDE;
    }

    /** @return the "filter-all" predicate. */
    public static Filter acceptNone() {
        return Filter.EXCLUDE;
    }

    /**
     * Returns a predicate that checks a CatalogInfo object's property for {@link
     * Object#equals(Object) equality} with the provided property value.
     *
     * <p>The <tt>property</tt> parameter may be specified as a "path" of the form "prop1.prop2". If
     * any of the resulting properties along the path result in null this method will return null.
     *
     * <p>Indexed access to nested list and array properties is supported through the syntax {@code
     * "prop1[M].prop2.prop3[N]"}, where {@code prop1} and {@code prop3} are list or array
     * properties, {@code M} is the index of the {@code prop2} element to retrieve from {@code
     * prop1}, and {@code N} is the index of array or list property {@code prop3} to retrieve.
     * Indexed access to {{java.util.Set}} properties is <b>not</b> supported.
     *
     * <p>Evaluation of nested properties for <b>any</b> member of a collection property (including
     * Array, List, and Set properties) is supported through the syntax {@code "colProp.name}, which
     * will evaluate to the first {@code name} property of the first {@code colProp} property that
     * matches the expected value. For example, {@code Filter filter = equal("styles.id", "id1")}
     * creates a predicate that evaluates to {@code true} if any style in the set of styles of a
     * layer has the given id.
     *
     * <p>
     *
     * <p>If the evaluated object property value and the argument value are not of the same type,
     * the returned {@code Predicate} will use the {@link Converters} framework to try to match the
     * two property types before performing an {@code Object.equals} check.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>Simple: {@code equal("id", "myId");}
     *   <li>Nested: {@code equal("resource.metadata.someKey", Boolean.TRUE);}
     *   <li>Any Collection (for List, Array, and Set properties): {@code equal("styles.name",
     *       "point");}: any style in the styles property whose name is "point"
     *   <li>Indexed (for List and Array properties): {@code equal("resource.attributes[1]",
     *       myAttribute);}
     *   <li>Combined: {@code equal("resource.attributes[1].minOccurs", Integer.valueOf(1));}
     * </ul>
     *
     * @param property the qualified property name of the predicate's input object to evaluate
     * @param expected the value to check the input object's property against
     * @see PropertyIsEqualTo
     */
    public static Filter equal(final String property, final Object expected) {
        return equal(property, expected, MatchAction.ANY);
    }

    public static Filter equal(
            final String property, final Object expected, final MatchAction matchAction) {
        final boolean matchCase = true;
        return factory.equal(
                factory.property(property), factory.literal(expected), matchCase, matchAction);
    }

    /**
     * @return a predicate that evaluates whether the given String {@code property} contains the
     *     required character string, in a <b>case insensitive</b> manner.
     */
    public static Filter contains(final String property, final String subsequence) {
        PropertyName propertyName = factory.property(property);
        return contains(propertyName, subsequence);
    }

    public static Filter contains(final PropertyName propertyName, final String subsequence) {
        String pattern = "*" + fixSpecials(subsequence) + "*";
        String wildcard = "*";
        String singleChar = "?";
        String escape = "\\";
        boolean matchCase = false;

        return factory.like(propertyName, pattern, wildcard, singleChar, escape, matchCase);
    }

    /**
     * convienience method to escape any character that is special to the regex system.
     *
     * @param inString the string to fix
     * @return the fixed string
     */
    private static String fixSpecials(final String inString) {
        StringBuffer tmp = new StringBuffer("");

        for (int i = 0; i < inString.length(); i++) {
            char chr = inString.charAt(i);

            if (isSpecial(chr)) {
                tmp.append("\\" + chr);
            } else {
                tmp.append(chr);
            }
        }

        return tmp.toString();
    }

    /**
     * convienience method to determine if a character is special to the regex system.
     *
     * @param chr the character to test
     * @return is the character a special character.
     */
    private static boolean isSpecial(final char chr) {
        return ((chr == '.')
                || (chr == '?')
                || (chr == '*')
                || (chr == '^')
                || (chr == '$')
                || (chr == '+')
                || (chr == '[')
                || (chr == ']')
                || (chr == '(')
                || (chr == ')')
                || (chr == '|')
                || (chr == '\\')
                || (chr == '&')
                || (chr == '}')
                || (chr == '{'));
    }

    public static Filter fullTextSearch(final String subsequence) {
        return contains(ANY_TEXT, subsequence);
    }

    /**
     * Returns a predicate that evaluates to {@code true} if each of its components evaluates to
     * {@code true}.
     *
     * <p>The components are evaluated in order, and evaluation will be "short-circuited" as soon as
     * a false predicate is found.
     */
    public static Filter and(Filter op1, Filter op2) {
        List<Filter> children = new ArrayList<Filter>();
        if (op1 instanceof And) {
            children.addAll(((And) op1).getChildren());
        } else {
            children.add(op1);
        }
        if (op2 instanceof And) {
            children.addAll(((And) op2).getChildren());
        } else {
            children.add(op2);
        }

        return factory.and(children);
    }

    /**
     * Returns a negated filter. If the filter was already a negation, its child fiter will be
     * returned instead (simplifying out the double negation)
     */
    public static Filter not(Filter filter) {
        if (filter instanceof Not) {
            return ((Not) filter).getFilter();
        }
        return factory.not(filter);
    }

    /**
     * Returns a predicate that evaluates to {@code true} if each of its components evaluates to
     * {@code true}.
     *
     * <p>The components are evaluated in order, and evaluation will be "short-circuited" as soon as
     * a false predicate is found.
     */
    public static Filter and(Filter... operands) {
        List<Filter> anded = Lists.newArrayList(operands);
        return factory.and(anded);
    }

    /**
     * Returns a predicate that evaluates to {@code true} if each of its components evaluates to
     * {@code true}.
     *
     * <p>The components are evaluated in order, and evaluation will be "short-circuited" as soon as
     * a false predicate is found.
     */
    public static Filter and(List<Filter> operands) {
        if (operands.size() == 0) {
            return Filter.INCLUDE;
        } else if (operands.size() == 1) {
            return operands.get(0);
        } else {
            return factory.and(operands);
        }
    }

    /**
     * Returns a predicate that evaluates to {@code true} if either of its components evaluates to
     * {@code true}.
     *
     * <p>The components are evaluated in order, and evaluation will be "short-circuited" as soon as
     * a true predicate is found.
     */
    public static Filter or(Filter op1, Filter op2) {
        List<Filter> children = new ArrayList<Filter>();
        if (op1 instanceof Or) {
            children.addAll(((Or) op1).getChildren());
        } else {
            children.add(op1);
        }
        if (op2 instanceof Or) {
            children.addAll(((Or) op2).getChildren());
        } else {
            children.add(op2);
        }
        return factory.or(children);
    }

    public static Filter or(Filter... operands) {
        List<Filter> ored = Lists.newArrayList(operands);
        return factory.or(ored);
    }

    public static Filter or(List<Filter> operands) {
        if (operands.size() == 0) {
            return Filter.EXCLUDE;
        } else if (operands.size() == 1) {
            return operands.get(0);
        } else {
            return factory.or(operands);
        }
    }

    public static Filter isNull(final String propertyName) {
        return factory.isNull(factory.property(propertyName));
    }

    public static SortBy asc(final String propertyName) {
        return sortBy(propertyName, true);
    }

    public static SortBy desc(final String propertyName) {
        return sortBy(propertyName, false);
    }

    public static SortBy sortBy(final String propertyName, final boolean ascending) {
        return factory.sort(propertyName, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING);
    }

    public static Filter isInstanceOf(Class clazz) {
        return factory.equals(
                factory.function("isInstanceOf", factory.literal(clazz)), factory.literal(true));
    }

    /**
     * Returns a predicate that checks a CatalogInfo object's property for inequality with the
     * provided property value.
     *
     * <p>The <tt>property</tt> parameter may be specified as a "path" of the form "prop1.prop2". If
     * any of the resulting properties along the path result in null this method will return null.
     *
     * <p>Indexed access to nested list and array properties is supported through the syntax {@code
     * "prop1[M].prop2.prop3[N]"}, where {@code prop1} and {@code prop3} are list or array
     * properties, {@code M} is the index of the {@code prop2} element to retrieve from {@code
     * prop1}, and {@code N} is the index of array or list property {@code prop3} to retrieve.
     * Indexed access to {{java.util.Set}} properties is <b>not</b> supported.
     *
     * <p>Evaluation of nested properties for <b>any</b> member of a collection property is at the
     * moment not supported
     *
     * <p>
     *
     * @param property the qualified property name of the predicate's input object to evaluate
     * @param expected the value to check the input object's property against
     * @see PropertyIsEqualTo
     */
    public static Filter notEqual(final String property, final Object expected) {
        return factory.notEqual(factory.property(property), factory.literal(expected));
    }
}
