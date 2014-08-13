/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import net.opengis.cat.csw20.CapabilitiesType;

import org.geoserver.csw.store.CatalogStore;

/**
 * The CSW GetCapabilities decorator extension interface
 * 
 * @author Alessio Fabiani - GeoSolutions
 */
public interface CapabilitiesDecorator {

    CapabilitiesType decorate(CapabilitiesType caps, CatalogStore store);
    
}
