/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.Serializable;
import java.lang.reflect.Field;
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
public abstract class ConformanceInfo<S extends ServiceInfo> implements Serializable {

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

    /** Returns the list of conformance classes that are configurable for this service. */
    public abstract List<APIConformance> configurableConformances();

    /**
     * Configuration for ServiceInfo.
     *
     * @param serviceInfo ServiceInfo configuration
     * @return List of enabled conformance
     */
    public abstract List<APIConformance> conformances(S serviceInfo);

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public abstract String getId();

    public Boolean isEnabled(APIConformance item) {
        try {
            Field field = this.getClass().getDeclaredField(item.getProperty());
            field.setAccessible(true);
            return (Boolean) field.get(this);
        } catch (NoSuchFieldException | ClassCastException | IllegalAccessException e) {
            throw new RuntimeException("Could not find field for conformance: " + item.getProperty(), e);
        }
    }

    public void setEnabled(APIConformance item, Boolean enabled) {
        try {
            Field field = this.getClass().getDeclaredField(item.getProperty());
            field.setAccessible(true);
            field.set(this, enabled);
        } catch (NoSuchFieldException | ClassCastException | IllegalAccessException e) {
            throw new RuntimeException("Could not find field for conformance: " + item.getProperty(), e);
        }
    }
}
