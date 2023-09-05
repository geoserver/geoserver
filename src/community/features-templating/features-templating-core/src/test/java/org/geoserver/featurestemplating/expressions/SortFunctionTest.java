/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;

import java.util.List;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.expression.Expression;
import org.junit.Test;

public class SortFunctionTest extends ListFunctionsTestSupport {

    @Test
    public void testSortFunction() {
        Expression sort = ff.function("sort", ff.literal("DESC"), ff.property("intValue"));
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) sort.evaluate(featureList);
        int value = 9;
        assertEquals(9, result.size());
        for (Object o : result) {
            SimpleFeature f = (SimpleFeature) o;
            assertEquals(value, f.getAttribute("intValue"));
            value--;
        }
        sort = ff.function("sort", ff.literal("ASC"), ff.property("intValue"));
        result = (List<Object>) sort.evaluate(result);
        assertEquals(9, result.size());
        value = 1;
        for (Object o : result) {
            SimpleFeature f = (SimpleFeature) o;
            assertEquals(value, f.getAttribute("intValue"));
            value++;
        }
    }
}
