/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

/** Aggregate operation able to find the Max value from a list of numeric value. */
class MaxOp extends MinMaxOp {

    @Override
    protected Number initialValue() {
        return Integer.MIN_VALUE;
    }

    @Override
    protected boolean updateValue(Number cur, Number newNumber) {
        return newNumber.doubleValue() > cur.doubleValue();
    }
}
