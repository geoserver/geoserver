/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.List;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Abstraction for a Min or Max aggregate operation. */
public abstract class MinMaxOp extends AggregationOp {

    private static final Logger LOGGER = Logging.getLogger(MinMaxOp.class);

    MinMaxOp() {
        super(null);
    }

    @Override
    protected Object aggregateInternal(List<Object> values) {
        Number cur = initialValue();
        for (Object o : values) {
            o = unpack(o);
            if (o != null && !Number.class.isAssignableFrom(o.getClass())) {
                String msg =
                        "Cannot compute min or max value of a list where there are non numeric values.";
                LOGGER.severe(msg);
                throw new UnsupportedOperationException(msg);
            }

            if (o != null) {
                Number otherN = (Number) o;
                if (updateValue(cur, otherN)) cur = otherN;
            }
        }
        return cur;
    }

    /**
     * Get the initial value used as a start point for the computation.
     *
     * @return the initial value.
     */
    protected abstract Number initialValue();

    /**
     * Return true if the current value should be updated, false otherwise.
     *
     * @param cur the current value.
     * @param newNumber the new number to test against the current value.
     * @return true if the test is successful false otherwise.
     */
    protected abstract boolean updateValue(Number cur, Number newNumber);
}
