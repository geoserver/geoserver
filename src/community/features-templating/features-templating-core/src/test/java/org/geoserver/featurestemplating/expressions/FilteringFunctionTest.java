/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

public class FilteringFunctionTest extends ListFunctionsTestSupport {

    @Test
    public void testFilteringFunction() {
        Expression filtering =
                ff.function("filter", ff.literal("stringValue = \"A\" OR doubleValue > 5.5 "));
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) filtering.evaluate(featureList);
        assertEquals(5, result.size());
        for (Object o : result) {
            SimpleFeature f = (SimpleFeature) o;
            assertTrue(
                    f.getAttribute("stringValue").equals("A")
                            || (Double) f.getAttribute("doubleValue") > 5.5);
        }
    }
}
