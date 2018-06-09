/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.Optional;

/**
 * Report status of installed modules and extensions.
 *
 * <p>Reporting static information such as module name and version, and dynamic information like
 * configuration and drivers.
 *
 * @author Morgan Thompson - Boundless
 */
public interface ModuleStatus {

    /**
     * Module identifier based on artifact bundle Example: <code>gs-main</code>, <code>gs-oracle
     * </code>
     */
    String getModule();
    /** Optional component identifier within module. Example: <code>rendering-engine</code> */
    Optional<String> getComponent();

    /**
     * Human readable name (from GeoServer documentation), or as defined in the extension pom.xml,
     * ie. <name>ArcSDE DataStore Extensions</name>
     */
    String getName();

    /** Human readable version, ie. for geotools it would be 15-SNAPSHOT * */
    Optional<String> getVersion();

    /** Returns whether the module is available to GeoServer * */
    boolean isAvailable();

    /** Returns whether the module is enabled in the current GeoServer configuration. * */
    boolean isEnabled();

    /**
     * Optional status message such as what Java rendering engine is in use, or the library path if
     * the module/driver is unavailable
     */
    Optional<String> getMessage();

    /** Optional relative link to GeoServer user manual */
    Optional<String> getDocumentation();
}
