/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.gml3;

import org.geotools.xml.Configuration;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.BasicComponentParameter;

/**
 * Pico parameter used for setter injection for {@link AbstractGeometryTypeBinding} to get around
 * that pico can't do composite injection, at least not hte version we use.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class WFSConfigurationParameter extends BasicComponentParameter {

    Configuration config;

    public WFSConfigurationParameter(Configuration config) {
        super(Configuration.class);
        this.config = config;
    }

    public boolean isResolvable(PicoContainer container, ComponentAdapter adapter, Class expectedType) {
        if (Configuration.class.isAssignableFrom(expectedType)) {
            return true;
        }
        return super.isResolvable(container, adapter, expectedType);
    };

    @Override
    public Object resolveInstance(PicoContainer container, ComponentAdapter adapter, Class expectedType) {
        if (Configuration.class.isAssignableFrom(expectedType)) {
            return config;
        }
        return super.resolveInstance(container, adapter, expectedType);
    }
}
