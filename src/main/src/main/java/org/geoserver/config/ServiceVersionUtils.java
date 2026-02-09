/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.ows.util.RequestUtils;
import org.geotools.util.Version;

/** Utility class for working with service versions and filtering based on ServiceInfo configuration. */
public class ServiceVersionUtils {

    /**
     * Returns the supported versions for a given service, filtering out disabled versions
     *
     * @param service The service id (e.g., "WMS", "WFS", "WCS")
     * @param serviceInfo The service configuration. If null, no filtering is applied
     * @return The list of supported enabled versions
     */
    public static List<String> getEnabledVersions(String service, ServiceInfo serviceInfo) {
        List<String> versions = RequestUtils.getSupportedVersions(service);

        if (serviceInfo != null && serviceInfo.getDisabledVersions() != null) {
            Set<String> disabledVersionStrings = serviceInfo.getDisabledVersions().stream()
                    .map(Version::toString)
                    .collect(Collectors.toSet());

            versions = versions.stream()
                    .filter(v -> !disabledVersionStrings.contains(v))
                    .collect(Collectors.toList());
        }

        return versions;
    }

    /**
     * Checks if a specific version is enabled for a service.
     *
     * @param version The version to check
     * @param serviceInfo The service configuration. If null, returns true (enabled by default)
     * @return true if the version is enabled, false if it's disabled
     */
    public static boolean isVersionEnabled(Version version, ServiceInfo serviceInfo) {
        if (serviceInfo == null || serviceInfo.getDisabledVersions() == null) {
            return true;
        }
        return !serviceInfo.getDisabledVersions().contains(version);
    }

    /**
     * Checks if a specific version string is enabled for a service.
     *
     * @param versionString The version string to check (e.g., "1.1.1")
     * @param serviceInfo The service configuration. If null, returns true (enabled by default)
     * @return true if the version is enabled, false if it's disabled
     */
    public static boolean isVersionEnabled(String versionString, ServiceInfo serviceInfo) {
        if (versionString == null) {
            return true;
        }
        return isVersionEnabled(new Version(versionString), serviceInfo);
    }
}
