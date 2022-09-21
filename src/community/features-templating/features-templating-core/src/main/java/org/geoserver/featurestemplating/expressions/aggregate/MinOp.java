/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

/** Aggregate operation that is able to find the minimum value in a list of numeric values. */
class MinOp extends MinMaxOp {

    @Override
    protected Number initialValue() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean updateValue(Number cur, Number newNumber) {
        return newNumber.doubleValue() < cur.doubleValue();
    }
}
