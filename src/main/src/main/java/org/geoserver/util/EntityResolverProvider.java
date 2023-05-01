/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.PreventLocalEntityResolver;
import org.xml.sax.EntityResolver;

/**
 * Creates an EntityResolver using geoserver configuration settings.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class EntityResolverProvider {

    private static final String ENTITY_RESOLUTION_ALLOWLIST = "ENTITY_RESOLUTION_ALLOWLIST";

    /** Allow list configuration from ENTITY_RESOLUTION_ALLOWLIST property. */
    static String[] ALLOW_LIST = entityResolutionAllowlist();

    /**
     * EntityResolve provided for use, acts as an override of settings and system properties.
     *
     * <p>Used by test cases to override PreventLocalEntityResolver.INSTANCE default.
     */
    private static EntityResolver entityResolver = null;

    /**
     * Limit external entity resolution to provided list, and support locations (w3c,OGC,INSPIRE),
     * and internal entity resolution, resolution to built-in xsd and GeoServer.
     */
    private final AllowListEntityResolver ALLOWLIST_ENTITY_RESOLVER;

    /** A entity resolver provider that always disables entity resolution */
    public static final EntityResolverProvider RESOLVE_DISABLED_PROVIDER =
            new EntityResolverProvider(null);

    private final GeoServer geoServer;

    public EntityResolverProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.ALLOWLIST_ENTITY_RESOLVER = new AllowListEntityResolver(geoServer);
    }

    /**
     * Provide default EntityResolver, used if global settings not provided explicit instructions.
     *
     * <p>Primarily used to stage an EntityResolver for test cases or local development.
     *
     * @param resolver Entity resolver
     */
    public static void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    /**
     * Obtain EntityResolver respecting GeoServer settings on accessing External Entities.
     *
     * <p>To use your own EntityResolver (in a test case) call {@link
     * #setEntityResolver(EntityResolver)}.
     *
     * <p>If the xml external entities enabled is set to true, no entity resolver is provided (and
     * XML entity resolution can work with any location including local files).
     *
     * <p>Defaults to PreventLocalEntityResolver allowing all http(s) entity resolution, while
     * preventing local file access. This implementation allows access to XSD files included in the
     * geoserver application.
     *
     * @return EntityResolver, or null if
     */
    public EntityResolver getEntityResolver() {
        if (geoServer != null) {
            Boolean externalEntitiesEnabled = geoServer.getGlobal().isXmlExternalEntitiesEnabled();
            if (externalEntitiesEnabled != null && externalEntitiesEnabled) {
                // XML parser is unrestricted, and can access any XSD location
                return null;
            }

            if (ALLOW_LIST != null) {
                // External entity resolution limited to those approved for use
                // those built-in to GeoSever, while restricting file access
                return ALLOWLIST_ENTITY_RESOLVER;
            }
        }
        if (entityResolver != null) {
            // override provided (usually by a test case)
            return entityResolver;
        } else {
            // Allows access to any http(s) location, and those built-in to GeoServer jars, while
            // restricting file access.
            return PreventLocalEntityResolver.INSTANCE;
        }
    }

    /**
     * Locations allowed for external entity expansion from application property
     * "ENTITY_RESOLUTION_ALLOWLIST".
     *
     * <ul>
     *   <li><code>"*" or undefined</code>: Allow all http(s) schema locations
     *   <li><code>"": Restrict to schemas provided by w3c, ogc and inspire</code>
     *   <li><code>
     *       "location1,location2": Restrict to the provided locations, and those list by w4c, ogc and inspire
     *       </code>
     * </ul>
     *
     * <p>The built-in list appended by {@link AllowListEntityResolver} is equivalent to: <code>
     * www.w3.org,schemas.opengis.net,www.opengis.net,inspire.ec.europa.eu/schemas</code> and the
     * proxy base url if known. This setting is used by {@link EntityResolverProvider} to limit
     * external entity resolution.
     *
     * @return Restrict external http(s) entity expansion to these external locations, or null to
     *     disable restriction.
     */
    public static String[] entityResolutionAllowlist() {
        String allowed = GeoServerExtensions.getProperty(ENTITY_RESOLUTION_ALLOWLIST);
        if (allowed == null || "*".equals(allowed.trim())) {
            // allow all external http(s) access
            return null;
        } else if (!"".equals(allowed.trim())) {
            // allow external http(s) access to ogc, w3c, and
            return new String[0];
        } else {
            return allowed.split("\\s*,\\s*|\\s+");
        }
    }
}
