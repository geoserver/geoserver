/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.junit.Assert.assertEquals;

import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;

public class NormalizingFilterVisitorTest {

    static final NormalizingFilterVisitor VISITOR = new NormalizingFilterVisitor();

    private static Filter normalize(Filter f) {
        return (Filter) f.accept(VISITOR, null);
    }

    @Test
    public void testEqualSwap() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("13 = foo"));
        Filter expected = ECQL.toFilter("foo = 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testGtSwap() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("13 > foo"));
        Filter expected = ECQL.toFilter("foo < 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testGteSwap() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("13 >= foo"));
        Filter expected = ECQL.toFilter("foo <= 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testLtSwap() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("13 < foo"));
        Filter expected = ECQL.toFilter("foo > 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testLteSwap() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("13 <= foo"));
        Filter expected = ECQL.toFilter("foo >= 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testPropertyLeft() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("foo = 13"));
        Filter expected = ECQL.toFilter("foo = 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testAndSort() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("foo = 1 AND bar = 2"));
        Filter expected = ECQL.toFilter("bar = 2 AND foo = 1");
        assertEquals(expected, normalized);
    }

    @Test
    public void testOrSort() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("foo = 1 OR bar = 2"));
        Filter expected = ECQL.toFilter("bar = 2 OR foo = 1");
        assertEquals(expected, normalized);
    }

    @Test
    public void testAndSwapSort() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("13 = foo AND bar = 'x'"));
        Filter expected = ECQL.toFilter("bar = 'x' AND foo = 13");
        assertEquals(expected, normalized);
    }

    @Test
    public void testOrSort3() throws Exception {
        // normalize flattens nested OR (via SimplifyingFilterVisitor), so expected must also be normalized
        Filter expected = normalize(ECQL.toFilter("a = 1 OR b = 2 OR c = 3"));
        assertEquals(expected, normalize(ECQL.toFilter("c = 3 OR a = 1 OR b = 2")));
        assertEquals(expected, normalize(ECQL.toFilter("b = 2 OR c = 3 OR a = 1")));
    }

    @Test
    public void testInSort() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("in3(foo,'c','a','b') = true"));
        Filter expected = ECQL.toFilter("in3(foo,'a','b','c') = true");
        assertEquals(expected, normalized);
    }

    @Test
    public void testInAlreadySorted() throws Exception {
        Filter normalized = normalize(ECQL.toFilter("in3(foo,'a','b','c') = true"));
        Filter expected = ECQL.toFilter("in3(foo,'a','b','c') = true");
        assertEquals(expected, normalized);
    }
}
