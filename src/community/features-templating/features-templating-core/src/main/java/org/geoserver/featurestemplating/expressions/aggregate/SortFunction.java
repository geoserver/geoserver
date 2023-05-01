/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import static org.geoserver.featurestemplating.expressions.aggregate.AggregationOp.unpack;
import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.VolatileFunction;

/**
 * Function able to sort the values in a list. The two parameters accepted are the sort order (ASC
 * or DESC) and and an optional property name to sort the current attribute on a nested value.
 */
public class SortFunction extends FunctionExpressionImpl implements VolatileFunction {

    private static FunctionName NAME =
            new FunctionNameImpl(
                    "sort",
                    parameter("result", Object.class),
                    parameter("order", String.class, 1, 1),
                    parameter("property", String.class, 0, 1));

    private enum Order {
        ASC,
        DESC;
    }

    public SortFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        Order order = getOrder();
        Expression pn = getPropertyName();
        return doSortNaturalOrder(object, order, pn);
    }

    private Order getOrder() {
        try {
            String strOrder = getParameters().get(0).evaluate(null, String.class);
            return Order.valueOf(strOrder.trim().toUpperCase());
        } catch (EnumConstantNotPresentException e) {
            throw new RuntimeException(
                    "Error while parsing the Sort order in the Sort function. Valid sort oder are ASC, DESC");
        }
    }

    private Expression getPropertyName() {
        Expression result = null;
        List<Expression> expressions = getParameters();
        if (expressions.size() > 1) result = expressions.get(1);
        return result;
    }

    private List<Object> doSortNaturalOrder(Object object, Order order, Expression propertyName) {
        List<Object> objectList;
        if (object instanceof Collection) objectList = new ArrayList<>((Collection<Object>) object);
        else if (object instanceof Object[]) objectList = Arrays.asList((Object[]) object);
        else objectList = Arrays.asList(object);
        Comparator<Object> comparator = new ComparableOrToStringComparator(propertyName);
        if (order.equals(Order.DESC)) comparator = comparator.reversed();
        objectList.sort(comparator);
        return objectList;
    }

    /**
     * Compare two comparable or convert them to String if they are not of Comparable type. It can
     * accept an expression to evaluate on the passed object to compare.
     */
    private static class ComparableOrToStringComparator implements Comparator {

        private Expression propertyName;

        ComparableOrToStringComparator(Expression pn) {
            this.propertyName = pn;
        }

        @Override
        public int compare(Object o1, Object o2) {
            if (propertyName != null) {
                o1 = unpack(propertyName.evaluate(o1));
                o2 = unpack(propertyName.evaluate(o2));
            }
            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                @SuppressWarnings("unchecked")
                int result = ((Comparable) o1).compareTo(o2);
                return result;
            } else return o1.toString().compareTo(o2.toString());
        }
    }
}
