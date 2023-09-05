/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import static org.geoserver.featurestemplating.expressions.aggregate.AggregationOp.toListObj;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.VolatileFunction;
import org.geotools.filter.FunctionExpressionImpl;

/**
 * A Function accepting as parameters an unlimited number of Expression that get concatenated so
 * that each of them receives as input the output of the previous expression.
 */
public class StreamFunction extends FunctionExpressionImpl implements VolatileFunction {

    public static FunctionName NAME = functionName("stream", "result:Object:1,1", "v:Object:1,");

    public StreamFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        List<Object> input = toListObj(object);
        @SuppressWarnings("unchecked")
        List<Object> result = new LinkedList<>(input);
        for (Expression exp : getParameters()) {
            if (exp instanceof SortFunction || exp instanceof FilteringFunction)
                // these functions support evaluating against a List
                result = toListObj(exp.evaluate(result));
            else
                // will evaluate each el in the list
                result = evaluateIterating(result, exp);
        }
        return result;
    }

    /**
     * Evaluates an expression on each object in the list.
     *
     * @param values the list of values.
     * @param expression the expression to evaluate.
     * @return the list of results of the evaluation of the expression on each element.
     */
    @SuppressWarnings("unchecked")
    private LinkedList<Object> evaluateIterating(List<Object> values, Expression expression) {
        LinkedList<Object> tmpResult = new LinkedList<>();
        for (Object o : values) {
            Object res = expression.evaluate(o);
            if (res instanceof Collection) tmpResult.addAll((List) res);
            else tmpResult.add(res);
        }
        return tmpResult;
    }
}
