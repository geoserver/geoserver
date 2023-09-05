/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.Arrays;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

/** Checks if an array contains null values */
public class ArrayHasNullFunction extends FunctionExpressionImpl {

    /** Get function name */
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "arrayhasnull",
                    parameter("result", Boolean.class),
                    parameter("arraytocheck", String[].class));

    /** Constructor */
    public ArrayHasNullFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        try {
            String[] values = getExpression(0).evaluate(feature, String[].class);
            if (values != null) {
                return Arrays.stream(values).anyMatch(workspace -> workspace == null);
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "ArrayHasNullFunction problem for argument #0 - expected type String[]", e);
        }
    }
}
