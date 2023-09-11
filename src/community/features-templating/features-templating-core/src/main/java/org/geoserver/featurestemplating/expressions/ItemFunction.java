/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.lang.reflect.Array;
import org.geotools.api.feature.Attribute;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

/**
 * Allows extraction of a given item from an array (as it's hard to do with a xpath, since the array
 * value is not quite the same as having an attribute with multiple repetitions)
 */
public class ItemFunction extends FunctionExpressionImpl {
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "item",
                    Object.class,
                    parameter("array", Object.class),
                    parameter("idx", Integer.class));

    public ItemFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        Object array = getExpression(0).evaluate(feature, Object.class);
        if (array instanceof Attribute) {
            array = ((Attribute) array).getValue();
        }
        Integer idx = getExpression(1).evaluate(feature, Integer.class);

        if (array == null) return null;
        if (!array.getClass().isArray())
            throw new IllegalArgumentException("First argument is not an array");

        if (idx == null) return null;

        return Array.get(array, idx);
    }
}
