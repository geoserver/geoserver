/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import java.util.List;
import org.geotools.api.filter.expression.Expression;
import org.junit.Assert;
import org.junit.Test;

public class StreamFunctionTest extends ListFunctionsTestSupport {

    @Test
    public void testStreamFunction() {
        Expression filtering =
                ff.function("filter", ff.literal("stringValue = \"A\" OR intValue >= 8 "));
        Expression sort = ff.function("sort", ff.literal("DESC"));
        Expression pn = ff.property("doubleValue");
        Expression stream = ff.function("stream", filtering, pn, sort);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) stream.evaluate(featureList);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(9.9, result.get(0));
        Assert.assertEquals(8.8, result.get(1));
        Assert.assertEquals(1.1, result.get(2));
    }
}
