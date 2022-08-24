/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Aggregate operation that removes duplicated values from the input list. */
class UniqueOp extends AggregationOp {

    UniqueOp() {
        super(null);
    }

    @Override
    protected Object aggregateInternal(List<Object> values) {
        Set<Object> set =
                values.stream()
                        .map(o -> unpack(o))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return new LinkedList<>(set);
    }
}
