/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

/**
 * The CapabilitiesErrorHandling class enumerates the modes of error handling GeoServer can use when
 * streaming XML documents (such as the WMS GetCapabilities response document.)
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public enum ResourceErrorHandling {
    /**
     * When this mode is active, GeoServer will respond to errors by stopping the normal XML
     * generation and going straight to an OGC error report of the issue. If the buffer strategy is
     * set to "SPEED" then this may result in invalid XML being sent to the client.
     */
    OGC_EXCEPTION_REPORT,

    /**
     * When this mode is active, GeoServer will respond to errors by "rolling back" XML to the end
     * of the previous successfully encoded element, effectively omitting elements that correspond
     * to misconfigured layers in the GeoServer catalog.
     */
    SKIP_MISCONFIGURED_LAYERS
}
