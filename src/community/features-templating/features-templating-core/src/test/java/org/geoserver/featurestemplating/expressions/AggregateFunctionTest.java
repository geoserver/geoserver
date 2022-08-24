/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.filter.expression.Expression;

public class AggregateFunctionTest extends ListFunctionsTestSupport {

    @Test
    public void testAggregateMinFunction() {
        Expression aggregate = buildNumericAggregate("MIN");
        Object result = aggregate.evaluate(featureList);
        Assert.assertEquals(1.1, result);
    }

    @Test
    public void testAggregateMaxFunction() {
        Expression aggregate = buildNumericAggregate("MAX");
        Object result = aggregate.evaluate(featureList);
        Assert.assertEquals(9.9, result);
    }

    @Test
    public void testAggregateAvgFunction() {
        Expression aggregate = buildNumericAggregate("AVG");
        Object result = aggregate.evaluate(featureList);
        Assert.assertEquals(6.60d, ((Double) result).doubleValue(), 0.00001);
    }

    @Test
    public void testAggregateUniqueFunction() {
        Expression aggregate = buildAggregate("UNIQUE", "dupProperty", "intValue > 3");
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) aggregate.evaluate(featureList);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("odd", result.get(0));
        Assert.assertEquals("even", result.get(1));
    }

    @Test
    public void testAggregateJoinFunction() {
        Expression aggregate = buildAggregate("JOIN(,)", "stringValue", "intValue < 6");
        Object result = aggregate.evaluate(featureList);
        Assert.assertEquals("E,D,C,B,A", result);
    }

    @Test
    public void testAggregateJoinFunctionDefSep() {
        Expression aggregate = buildAggregate("JOIN", "stringValue", "intValue < 6");
        Object result = aggregate.evaluate(featureList);
        Assert.assertEquals("E D C B A", result);
    }

    private Expression buildNumericAggregate(String aggrParam) {
        return buildAggregate(aggrParam, "doubleValue", "stringValue = \"A\" OR intValue >= 8 ");
    }

    private Expression buildAggregate(String aggrParam, String propertyName, String filter) {
        Expression filtering = ff.function("filter", ff.literal(filter));
        Expression sort = ff.function("sort", ff.literal("DESC"));
        Expression pn = ff.property(propertyName);
        Expression stream = ff.function("stream", filtering, pn, sort);
        Expression aggregate = ff.function("aggregate", stream, ff.literal(aggrParam));
        return aggregate;
    }
}
