/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * EntityResolver implementation to prevent usage of external entities.
 * 
 * When parsing an XML entity, the empty InputSource returned by this resolver provokes 
 * throwing of a java.net.MalformedURLException, which can be handled appropriately.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class NoExternalEntityResolver implements EntityResolver {

    private static final Logger LOGGER = Logging.getLogger(NoExternalEntityResolver.class);
    
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("resolveEntity request: publicId=" + publicId + ", systemId=" + systemId);
        }
        
        // allow schema parsing for validation
        if (systemId != null && systemId.endsWith(".xsd")) {
            return null;
        }
        
        // do not allow external entities
        return new InputSource();
    }
}