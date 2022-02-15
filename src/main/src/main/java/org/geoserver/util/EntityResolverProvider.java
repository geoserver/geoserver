/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import org.geoserver.config.GeoServer;
import org.geotools.util.PreventLocalEntityResolver;
import org.xml.sax.EntityResolver;

/**
 * Creates an EntityResolver using geoserver configuration settings.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class EntityResolverProvider {

    /** Defaults to PreventLocalEntityResolver which local file access. */
    private static EntityResolver entityResolver = PreventLocalEntityResolver.INSTANCE;

    /** Used for internal entity resolution, resolution to built-in xsd and GeoServer. */
    private final InternalEntityResolver INTERNAL_ENTITY_RESOLVER;

    /** A entity resolver provider that always disables entity resolution */
    public static final EntityResolverProvider RESOLVE_DISABLED_PROVIDER =
            new EntityResolverProvider(null);

    private final GeoServer geoServer;

    public EntityResolverProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.INTERNAL_ENTITY_RESOLVER = new InternalEntityResolver(geoServer);
    }

    /**
     * Provide default EntityResolver, used if global settings not provided explicit instructions.
     *
     * <p>Primarily used to stage an EntityResolver for test cases.
     *
     * @param resolver Entity resolver
     */
    public static void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    /**
     * Obtain EntityResolver respecting GeoServer settings on accessing External Entities.
     *
     * @return EntityResolver
     */
    public EntityResolver getEntityResolver() {
        if (geoServer != null) {
            Boolean externalEntitiesEnabled = geoServer.getGlobal().isXmlExternalEntitiesEnabled();
            if (externalEntitiesEnabled != null && externalEntitiesEnabled) {
                // XML parser will try to resolve entities
                return null;
            }

            if (geoServer.getSettings() != null
                    && geoServer.getSettings().getProxyBaseUrl() != null) {
                return INTERNAL_ENTITY_RESOLVER;
            }
        }
        return entityResolver;
    }

    /**
     * GeoSever settings on accessing external entities is strict, skipping validation recommended.
     *
     * @return Skip validation to avoid use of strict entity resolver.
     */
    public boolean skipValidation() {
        if (geoServer != null) {
            Boolean externalEntitiesEnabled = geoServer.getGlobal().isXmlExternalEntitiesEnabled();
            if (externalEntitiesEnabled != null && externalEntitiesEnabled) {
                return false;
            }
            if (geoServer.getSettings() != null
                    && geoServer.getSettings().getProxyBaseUrl() != null) {
                return false;
            }
        }
        return true;
    }
}
