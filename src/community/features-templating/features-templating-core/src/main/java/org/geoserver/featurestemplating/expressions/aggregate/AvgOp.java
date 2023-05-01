/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.List;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Aggregate operation to compute the avg of a list of numeric values. */
class AvgOp extends AggregationOp {

    AvgOp() {
        super(null);
    }

    private static final Logger LOGGER = Logging.getLogger(AvgOp.class);

    @Override
    protected Object aggregateInternal(List<Object> list) {
        double sum = 0, avg;
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            o = unpack(o);
            if (!Number.class.isAssignableFrom(o.getClass())) {
                String msg = "Cannot compute avg of a non numeric value";
                LOGGER.severe(msg);
                throw new UnsupportedOperationException(msg);
            }
            sum = sum + ((Number) list.get(i)).doubleValue();
        }
        avg = sum / list.size();
        return avg;
    }
}
