/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.logging.Logger;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.VolatileFunction;

/**
 * Function capable of performing aggregate operations over a List of values. Supported aggregate
 * operations are: MAX,MIN,AVG,UNIQUE,JOIN.
 */
public class AggregateFunction extends FunctionExpressionImpl implements VolatileFunction {

    private static FunctionName NAME =
            new FunctionNameImpl(
                    "aggregate",
                    parameter("result", Object.class),
                    parameter("value", Object.class),
                    parameter("aggregationType", String.class, 1, 1));

    public AggregateFunction() {
        super(NAME);
    }

    private static final Logger LOGGER = Logging.getLogger(AggregateFunction.class);

    @Override
    public Object evaluate(Object object) {
        Expression exp = getParameters().get(0);
        String aggrType = getParameters().get(1).evaluate(null, String.class);
        LOGGER.fine(() -> "Found aggregate type " + aggrType);
        AggregationOp aggregationOp = AggregateOpFactory.createOperation(aggrType);
        return aggregationOp.aggregate(exp.evaluate(object));
    }
}
