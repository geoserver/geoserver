/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.geoserver.config.ServiceInfo;

/**
 * Service configuration for a capability or feature, identified by conformance class.
 *
 * <p>OGC API Web API standards are defined as collection of modules that assembled into a WEB API. The resulting Web
 * API is defined in an {@link io.swagger.v3.oas.models.OpenAPI} document.
 *
 * <p>OGC API {@link ConformanceDocument} provides a high level summary listing "conformance classes" indicating
 * functionality following an official standard, or community standard. This base can be used to check
 * {@link APIConformance} for a specific ServiceInfo. This allows a SORT_BY APIConformance for each of FeatureService,
 * STACService, and RecordsService.
 *
 * <p>GeoServer allows you to select which service modules are enabled, and manage any configuration associated with
 * their use.
 *
 * <p>Open Web Services can also be extended, using service profiles, which can be managed in the same fashion using a
 * ConformanceInfo.
 *
 * <p>Generic / abstract conformance configuration, stored in ServiceInfo.
 */
public class ConformanceInfo<S extends ServiceInfo> implements Serializable {

    /**
     * Checks conformance configuration, to see if enabled.
     *
     * @param enabled Enabled status, if {@code null} conformance default used
     * @param conformance APIConformance used to determine default enabled status
     * @return enabled status
     */
    protected boolean isEnabled(S serviceInfo, Boolean enabled, APIConformance conformance) {
        if (enabled == null) {
            if (serviceInfo.isCiteCompliant()) {
                return conformance.getLevel().isEndorsed()
                        && conformance.getLevel().isStable();
            } else {
                return conformance.getLevel().isStable();
            }
        } else {
            return enabled;
        }
    }

    /**
     * Enabled is based on conformance level information and serviceInfo settings.
     *
     * <ul>
     *   <li>If cite compliance {@code true}, conformance must be endorsed and stable.
     *   <li>Or if cite compliance{@code false} conformance is only required to be stable.
     * </ul>
     *
     * @param conformance APIConformance
     * @return {@code true} if conformance is enabled based on service info settings.
     */
    protected boolean enabledDefault(S serviceInfo, APIConformance conformance) {
        if (conformance == null) {
            return false;
        }
        // default enabled based on conformance level
        if (serviceInfo.isCiteCompliant()) {
            return conformance.getLevel().isEndorsed() && conformance.getLevel().isStable();
        } else {
            return conformance.getLevel().isStable();
        }
    }
    /**
     * Check if configuration as a whole is enabled, or may be skipped.
     *
     * <p>The default implementation uses {@link ServiceInfo#isEnabled()}, override for a more specific check.
     *
     * @return {@code true} if configuration is enabled for use.
     */
    public boolean isEnabled(S serviceInfo) {
        return serviceInfo.isEnabled();
    }

    /**
     * Configuration for ServiceInfo.
     *
     * @param serviceInfo ServiceInfo configuration
     * @return List of enabled conformance
     */
    public List<APIConformance> conformances(S serviceInfo) {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        return sb.toString();
    }
}
