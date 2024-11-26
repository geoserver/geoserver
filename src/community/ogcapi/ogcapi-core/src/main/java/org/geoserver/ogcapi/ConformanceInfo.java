package org.geoserver.ogcapi;

import org.apiguardian.api.API;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geotools.util.Converters;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a service capability or feature, identified by conformance class.
 *
 * OGC API Web API standards are defined as collection of modules that assembled into a WEB API.
 * GeoServer allows you to select which service modules are enabled, and manage any configuration
 * associated with their use.
 *
 * Open Web Services can also be extended, using service profiles, which can be managed in the same
 * fashion using a ConformanceInfo.
 *
 * Generic / abstract conformance configuration, stored in ServiceInfo.
 */
public class ConformanceInfo<S extends ServiceInfo> implements Serializable {
    final String metadataKey;
    //final APIConformance[] defaultConformance;

    /**
     * Enable and configure service functionality by conformance class.
     *
     * The default configuration is used to determine if this conformance should
     * enable automaticly (if no configuration has been provided by the user).
     *
     * @param metadataKey Storage key for metadata map
     * @param defaultConformance Conformance classs used to determine default enabled status
     */
    public ConformanceInfo(String metadataKey){
        if (metadataKey == null){
            throw new NullPointerException("metadata key is null");
        }
        this.metadataKey = metadataKey;
        // this.defaultConformance = defaultConformance;
    }

    /**
     * Storage key.
     *
     * @return conformance class identifier.
     */
    public String getMetadataKey() {
        return metadataKey;
    }

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
                return conformance.getLevel().isEndorsed() && conformance.getLevel().isStable();
            }
            else {
                return conformance.getLevel().isStable();
            }
        }
        else {
            return enabled;
        }
    }

    /**
     * Enabled is based on conformance level information and serviceInfo settings.
     * <ul>
     *     <li>If cite compliance {@code true}, conformance must be endorsed and stable.</li>
     *     <li>Or if cite compliance{@code false} conformance is only required to be stable.</li>
     * </ul>
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
        }
        else {
            return conformance.getLevel().isStable();
        }
    }
    /**
     * Check if configuration as a whole is enabled, or may be skipped.
     *
     * The default implementation uses {@link ServiceInfo#isEnabled()}, override for a
     * more specific check.
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
        sb.append(" ");
        sb.append(this.metadataKey);
        return sb.toString();
    }
}