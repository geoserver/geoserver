/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.List;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * String join aggregate operation. It accept a param to specify a separator to be used in the
 * joining. If no param is passed default is white space.
 */
class JoinOp extends AggregationOp {

    JoinOp(String params) {
        super(params);
    }

    private static final Logger LOGGER = Logging.getLogger(JoinOp.class);

    @Override
    protected Object aggregateInternal(List<Object> values) {
        String sep = params == null ? " " : params;
        LOGGER.fine(() -> ("Separator is " + (sep.equals(" ") ? "blank space" : sep)));
        int size = values.size();
        StringBuilder sb = new StringBuilder();
        int last = size - 1;
        for (int i = 0; i < size; i++) {
            Object o = values.get(i);
            o = unpack(o);
            sb.append(o);
            if (i < last) sb.append(sep);
        }
        return sb.toString();
    }
}
