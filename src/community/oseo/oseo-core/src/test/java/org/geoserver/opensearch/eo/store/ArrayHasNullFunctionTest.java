package org.geoserver.opensearch.eo.store;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

public class ArrayHasNullFunctionTest {
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Test
    public void testEvaluate() throws Exception {

        String[] workspaces = {"test"};
        Expression workspaceLiteral = FF.literal(workspaces);
        Function function = FF.function("arrayhasnull", workspaceLiteral);
        assertFalse((Boolean) function.evaluate(null));
        String[] workspaces2 = {null, "test"};
        Expression workspaceLiteral2 = FF.literal(workspaces2);
        Function function2 = FF.function("arrayhasnull", workspaceLiteral2);
        assertTrue((Boolean) function2.evaluate(null));
        String[] workspaces3 = null;
        Expression workspaceLiteral3 = FF.literal(workspaces3);
        Function function3 = FF.function("arrayhasnull", workspaceLiteral3);
        assertFalse((Boolean) function3.evaluate(workspaceLiteral3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullArgument() throws Exception {
        ArrayHasNullFunction arrayHasNullFunction = new ArrayHasNullFunction();
        Expression workspaces = null;
        Function function = FF.function("arrayhasnull", workspaces);
        arrayHasNullFunction.evaluate(null);
    }
}
