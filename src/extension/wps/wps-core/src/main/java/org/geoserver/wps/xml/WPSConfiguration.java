/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.xml;

import java.util.Map;
import org.geotools.wps.WPS;
import org.picocontainer.MutablePicoContainer;

public class WPSConfiguration extends org.geotools.wps.WPSConfiguration {

    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);
        
        //binding overrides
        bindings.put( WPS.ComplexDataType, ComplexDataTypeBinding.class );
    }
    
    @Override
    protected void configureContext(MutablePicoContainer container) {
        super.configureContext(container);

        container.registerComponentInstance(new org.geoserver.wcs.xml.v1_1_1.WCSParserDelegate());
        container.registerComponentInstance(new org.geoserver.wcs.xml.v1_0_0.WCSParserDelegate());
    }
}
