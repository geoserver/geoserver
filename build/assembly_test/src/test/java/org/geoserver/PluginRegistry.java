/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for plugin-specific smoke test customizations:
 *
 * <ul>
 *   <li>extra plugin ZIP dependencies that must be unpacked before startup
 *   <li>custom testers for plugins needing setup or verification beyond the default WMS smoke test
 * </ul>
 */
final class PluginRegistry {

    private static final AbstractPluginTester DEFAULT_TESTER = new DefaultPluginTester();
    private static final Map<String, List<String>> DEPENDENCIES = new HashMap<>();
    private static final Map<String, AbstractPluginTester> TESTERS = new HashMap<>();

    static {
        // dependencies
        DEPENDENCIES.put("csw-iso", List.of("csw"));
        DEPENDENCIES.put("dxf", List.of("wps"));
        DEPENDENCIES.put("geopkg", List.of("wps"));
        DEPENDENCIES.put("gpx", List.of("wps"));
        DEPENDENCIES.put("gdal-wps", List.of("wps", "gdal-wcs"));
        DEPENDENCIES.put("importer-jdbc", List.of("importer"));
        DEPENDENCIES.put("mbtiles", List.of("wps"));
        DEPENDENCIES.put("monitor-kafka", List.of("monitor"));
        DEPENDENCIES.put("monitor-micrometer", List.of("monitor"));
        DEPENDENCIES.put("netcdf-ghrsst", List.of("netcdf-out"));
        DEPENDENCIES.put("wps-cluster-hazelcast", List.of("wps"));
        DEPENDENCIES.put("wps-download", List.of("wps"));
        DEPENDENCIES.put("wps-download-netcdf", List.of("wps-download", "netcdf-out"));
        DEPENDENCIES.put("wps-jdbc", List.of("wps"));
        DEPENDENCIES.put("wps-longitudinal-profile", List.of("wps"));
        DEPENDENCIES.put("wps-remote", List.of("wps"));
        DEPENDENCIES.put("ogr-wps", List.of("wps", "ogr-wfs"));
        DEPENDENCIES.put("features-templating", List.of("ogcapi-features"));

        // custom testers
        TESTERS.put("wps-jdbc", new WPSJDBCTester());
        TESTERS.put("acl", new GeoServerAclTester());
    }

    private PluginRegistry() {}

    /** Returns the plugin dependencies, that is, the other plugins that need to be installed for the plugin to work */
    static List<String> dependenciesFor(String pluginName) {
        return DEPENDENCIES.get(pluginName);
    }

    /** Returns the plugin-specific tester, or the default smoke tester when none is registered. */
    static AbstractPluginTester testerFor(String pluginName) {
        return TESTERS.getOrDefault(pluginName, DEFAULT_TESTER);
    }
}
