/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions.aggregate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.geotools.api.feature.Property;
import org.geotools.feature.NameImpl;

/**
 * Abstraction for an Aggregate Operation. An aggregate operation might have params and that are
 * stored if present in a string attribute. It is the implementation that needs to convert it as
 * needed.
 */
abstract class AggregationOp {

    protected String params;

    AggregationOp(String params) {
        this.params = params;
    }

    protected abstract Object aggregateInternal(List<Object> values);

    Object aggregate(Object o) {
        return aggregateInternal(toListObj(o));
    }

    @SuppressWarnings("unchecked")
    static List<Object> toListObj(Object object) {
        List<Object> objectList;
        if (object instanceof Collection) objectList = new ArrayList<>((Collection<Object>) object);
        else if (object instanceof Object[]) objectList = Arrays.asList((Object[]) object);
        else objectList = Arrays.asList(object);
        return objectList;
    }

    /** Unpacks value from attribute container */
    static Object unpack(Object value) {

        if (value instanceof org.geotools.api.feature.ComplexAttribute) {
            Property simpleContent =
                    ((org.geotools.api.feature.ComplexAttribute) value)
                            .getProperty(new NameImpl("simpleContent"));
            if (simpleContent == null) {
                return null;
            } else {
                return simpleContent.getValue();
            }
        }

        if (value instanceof org.geotools.api.feature.Attribute) {
            return ((org.geotools.api.feature.Attribute) value).getValue();
        }

        return value;
    }
}
