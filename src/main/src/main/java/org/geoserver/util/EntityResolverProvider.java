/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import org.geoserver.config.GeoServer;
import org.geotools.factory.GeoTools;
import org.geotools.xml.PreventLocalEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;


/**
 * Creates an EntityResolver using geoserver configuration settings.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class EntityResolverProvider {
    
    /**
     * A entity resolver provider that always disabled entity resolution
     */
    public static final EntityResolverProvider RESOLVE_DISABLED_PROVIDER = new EntityResolverProvider(
            null);

    /**
     * In IDEs during development GeoTools sources can be in the classpath of GeoServer tests, this
     * resolver allows them to be resolved while blocking the rest
     */
    public static final EntityResolver RESOLVE_DISABLED_PROVIDER_DEVMODE = new PreventLocalEntityResolver() {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (DEVELOPER_MODE && isLocalGeoToolsSchema(null, systemId)) {
                return null;
            }

            return super.resolveEntity(publicId, systemId);
        }

        @Override
        public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
            if (DEVELOPER_MODE && isLocalGeoToolsSchema(baseURI, systemId)) {
                return null;
            }
            
            return super.resolveEntity(name, publicId, baseURI, systemId);
        }

        private boolean isLocalGeoToolsSchema(String baseURI, String systemId) {
            if (systemId.startsWith("file:/")) {
                return isLocalGeotoolsSchema(systemId);
            } else if (!systemId.contains("://") && baseURI != null) {
                // location relative to a baseURI
                return isLocalGeotoolsSchema(baseURI);
            }
            return false;
        }

        private boolean isLocalGeotoolsSchema(String path) {
            // Windows case insensitive filesystem work-around
            path = path.toLowerCase();
            // Match the GeoTools locations having schemas we resolve against
            return path.matches(".*modules[\\\\/]extension[\\\\/]xsd[\\\\/].*\\.xsd") ||
                    path.matches(".*modules[\\\\/]ogc[\\\\/].*\\.xsd");
        }
    };

    /**
     * Set this to true to allow resolution of any XSD file 
     */
    public static boolean DEVELOPER_MODE = false;


    private GeoServer geoServer;
    
    public EntityResolverProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
    }
    
    
    
    public EntityResolver getEntityResolver() {
        if (geoServer != null) {
            Boolean externalEntitiesEnabled = geoServer.getGlobal().isXmlExternalEntitiesEnabled();
            if (externalEntitiesEnabled != null && externalEntitiesEnabled.booleanValue()) {
                // XML parser will try to resolve entities
                return null;
            }
        }
        
        if (DEVELOPER_MODE) {
            return RESOLVE_DISABLED_PROVIDER_DEVMODE;
        } else {
            // default behaviour: entities disabled
            return PreventLocalEntityResolver.INSTANCE;
        }
    } 
}