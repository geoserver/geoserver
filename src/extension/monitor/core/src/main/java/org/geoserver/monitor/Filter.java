/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.Serializable;
import org.geoserver.monitor.Query.Comparison;

/**
 * Filter used in a {@link Query}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class Filter implements Serializable {

    Object left, right;
    Comparison type;

    public Filter(Object left, Object right, Comparison type) {
        this.left = left;
        this.right = right;
        this.type = type;
    }

    protected Filter() {}

    public Object getLeft() {
        return left;
    }

    public Object getRight() {
        return right;
    }

    public Comparison getType() {
        return type;
    }

    public And and(Filter other) {
        return new And(this, other);
    }

    public Or or(Filter other) {
        return new Or(this, other);
    }

    public void accept(FilterVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        return left + " " + type + " " + right;
    }
}
