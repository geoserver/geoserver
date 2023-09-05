/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import static org.geoserver.featurestemplating.expressions.aggregate.AggregationOp.toListObj;
import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.VolatileFunction;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;

/**
 * A Function that can receive as a param a filter as a literal. Literal value in the argument must
 * be passed inside double quotes (eventually escaped).
 */
public class FilteringFunction extends StringCQLFunction implements VolatileFunction {

    private static final FunctionName NAME =
            new FunctionNameImpl(
                    "filter",
                    parameter("result", Object.class),
                    parameter("filter", String.class, 1, 1));

    private static final Logger LOGGER = Logging.getLogger(FilteringFunction.class);

    public FilteringFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        List<Object> values = new ArrayList<>();
        String txtFilter = getExpression(0).evaluate(null, String.class);
        try {
            // not ideal but the compiler doesn't allow to passe single quotes inside a literal.
            txtFilter = fixLiterals(txtFilter);
            Filter filter = cqlToFilter(txtFilter);
            List<Object> list = toListObj(object);
            for (Object o : list) {
                if (filter.evaluate(o)) {
                    values.add(o);
                }
            }
        } catch (CQLException e) {
            throw new RuntimeException(
                    "The argument of the filter function is not a valid CQL filter");
        }
        return values;
    }

    /**
     * Removes the double quotes from the literals in text filters to replace them with single
     * quotes.
     *
     * @param textFilter the text filter.
     * @return the text filter with literals in single quotes.
     */
    private String fixLiterals(String textFilter) {
        StringBuilder sb = new StringBuilder();
        LOGGER.fine(() -> "Fixing literals in Filtering function...");
        for (char c : textFilter.toCharArray()) {
            if (c != '"') {
                sb.append(c);
            } else {
                LOGGER.fine(() -> "Replacing double quotes with single quotes");
                sb.append('\'');
            }
        }
        return sb.toString();
    }
}
