/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import org.geoserver.config.GeoServer;
import org.xml.sax.EntityResolver;


/**
 * Creates an EntityResolver using geoserver configuration settings.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class EntityResolverProvider {
    
    private GeoServer geoServer;
    
    public EntityResolverProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
    }
    
    public EntityResolver getEntityResolver() {
        Boolean externalEntitiesEnabled = geoServer.getGlobal().isXmlExternalEntitiesEnabled();
        if (externalEntitiesEnabled != null && externalEntitiesEnabled.booleanValue()) {
            // XML parser will try to resolve entities
            return null;
        } else {
            // default behaviour: entities disabled
            return new NoExternalEntityResolver();
        }
    } 
}