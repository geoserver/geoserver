/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.Serializable;
import java.util.List;

/**
 * Container for the default value configurations for all dimensions associated to a ResourceInfo
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultValueConfigurations implements Serializable, Cloneable {
    private static final long serialVersionUID = -5773499155045618173L;

    public static final String KEY = "DynamicDefaultValues";

    List<DefaultValueConfiguration> configurations;

    public DefaultValueConfigurations(List<DefaultValueConfiguration> configurations) {
        super();
        this.configurations = configurations;
    }

    public List<DefaultValueConfiguration> getConfigurations() {
        return configurations;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "DefaultValueConfigurations [configurations=" + configurations + "]";
    }
}
