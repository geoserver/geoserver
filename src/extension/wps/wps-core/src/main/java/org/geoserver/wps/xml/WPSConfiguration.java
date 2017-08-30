/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.xml;

import java.util.Map;

import org.geoserver.wfs.xml.v1_0_0.GetFeatureTypeBinding;
import org.geotools.wfs.WFSParserDelegate;
import org.geotools.wfs.v1_0.WFS;
import org.geotools.wfs.v1_1.WFSConfiguration;
import org.geotools.wps.WPS;
import org.geotools.xml.XSDParserDelegate;
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
        container.registerComponentInstance(new org.geoserver.wcs2_0.xml.WCSParserDelegate());
        // replace WFSParserDelegate from GeoTools with a new one using GeoServer GetFeatureTypeBinding,
        // able to parse viewParams attribute and enable usage of SQL views
        Object wfs = container.getComponentInstanceOfType(WFSParserDelegate.class);
        container.unregisterComponentByInstance(wfs);
        container.registerComponentInstance(new XSDParserDelegate(new WFSConfiguration() {

            @Override
            protected void configureBindings(MutablePicoContainer container) {
                super.configureBindings(container);
                container.registerComponentImplementation(WFS.GetFeatureType, GetFeatureTypeBinding.class);
            }
            
        }));
    }
}
