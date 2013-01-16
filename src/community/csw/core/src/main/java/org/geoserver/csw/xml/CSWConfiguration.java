/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.xml;

import java.util.Map;

import org.picocontainer.MutablePicoContainer;

public class CSWConfiguration extends org.geotools.csw.CSWConfiguration {

    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);
        
        //binding overrides
    }
    
    @Override
    protected void configureContext(MutablePicoContainer container) {
        super.configureContext(container);

    }
}
