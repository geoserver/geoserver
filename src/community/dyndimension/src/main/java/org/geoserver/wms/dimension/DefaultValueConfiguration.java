/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.Serializable;

/**
 * Configures the dynamic dimension values for a specific dimension
 *
 * @author Andrea Aime - GeoSolutions
 */
public final class DefaultValueConfiguration implements Serializable, Cloneable {
    private static final long serialVersionUID = -4294430917350930217L;

    public enum DefaultValuePolicy {
        /** Use the WMS standard default value, no dynamic computation */
        STANDARD,
        /** Apply the WMS standard policy against a restricted domain */
        LIMIT_DOMAIN,
        /** Compute the default value as a ECQL expression of the other dimensions */
        EXPRESSION
    }

    String dimension;

    DefaultValuePolicy policy;

    String defaultValueExpression;

    /** */
    public DefaultValueConfiguration(String dimension, DefaultValuePolicy policy) {
        this.dimension = dimension;
        this.policy = policy;
    }

    public DefaultValueConfiguration(String dimension, String defaultValueExpression) {
        this.dimension = dimension;
        this.policy = DefaultValuePolicy.EXPRESSION;
        this.defaultValueExpression = defaultValueExpression;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public DefaultValuePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(DefaultValuePolicy policy) {
        this.policy = policy;
    }

    public String getDefaultValueExpression() {
        return defaultValueExpression;
    }

    public void setDefaultValueExpression(String defaultValue) {
        this.defaultValueExpression = defaultValue;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "DefaultValueConfiguration [dimension="
                + dimension
                + ", policy="
                + policy
                + ", defaultValueExpression="
                + defaultValueExpression
                + "]";
    }
}
