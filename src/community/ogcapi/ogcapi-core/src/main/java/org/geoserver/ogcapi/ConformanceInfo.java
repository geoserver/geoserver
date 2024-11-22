package org.geoserver.ogcapi;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geotools.util.Converters;

import java.io.Serializable;
import java.util.HashMap;
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
public class ConformanceInfo<S extends ServiceInfo> {
    private static String ENABLED = "enabled";

    final S serviceInfo;
    final String metadataKey;
    final APIConformance defaultConformance;

    /**
     * Enable and configure service functionality by conformance class.
     *
     * The default configuration is used to determine if this conformance should
     * enable automaticly (if no configuration has been provided by the user).
     *
     * @param metadataKey Storage key for metadata map
     * @param defaultConformance Conformance class used to determine default enabled status
     * @param serviceInfo Service being configured
     */
    public ConformanceInfo(String metadataKey, APIConformance defaultConformance, S serviceInfo){
        if (metadataKey == null){
            throw new NullPointerException("metadata key is null");
        }
        this.metadataKey = metadataKey;
        this.defaultConformance = defaultConformance;
        if (serviceInfo == null){
            throw new NullPointerException("serviceInfo is null");
        }
        this.serviceInfo = serviceInfo;
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
     * ServiceInfo configuration.
     *
     * @return Service being configured.
     */
    public S getServiceInfo() {
        return serviceInfo;
    }


    /**
     * Map used for conformance configuration in metadata map.
     *
     * This map is maintained in the {@link ServiceInfo#getMetadata()} map for backwards compatibility.
     *
     * @return map used for conformance configuration
     */
    protected Map<String,Object> configuration() {
        MetadataMap metadata = serviceInfo.getMetadata();
        synchronized ( metadata ) {
            HashMap<String, Object> configuration = serviceInfo.getMetadata().get(metadataKey,HashMap.class);
            if (configuration == null) {
                configuration = new HashMap<>();
                serviceInfo.getMetadata().put(metadataKey, configuration);
            }
            return configuration;
        }
    }

    /**
     * Get configuration value for key.
     *
     * @param key
     * @param clazz Cast value to this class.
     * @return value for key, or null if not set (or cannot be converted to requested type).
     * @param <T>
     */
    protected <T> T get(String key, Class<T> clazz) {
        Object obj = configuration().get(key);
        if (obj == null) {
            return null;
        }
        return Converters.convert(obj, clazz);
    }

    protected void put(String key, Object value) {
        configuration().put(key,value);
    }

    /**
     * Checks conformance configuration, to see if key is enabled.
     *
     * If they key is not set, the default value is based on the conformance level information.
     *
     * @param conformance APIConformance to check
     * @return {@code true} if conformance is enabled
     */
    protected boolean isEnabled(APIConformance conformance) {
        return isEnabled(conformance.getKey(),conformance);
    }

    /**
     * Checks conformance configuration, to see if key is enabled.
     *
     * @param key configuration key
     * @param conformance APIConformance used to determine default value
     * @return if conformance
     */
    protected boolean isEnabled(String key, APIConformance conformance) {
        Boolean enabled = get(key,Boolean.class);
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
     * Set conformance enabled, using the conformance key.
     *
     * @param conformance APIConformance
     * @param enabled Enable status
     */
    protected void setEnabled(APIConformance conformance, boolean enabled) {
        put(conformance.getKey(),enabled);
    }
    /**
     * Set conformance metadata configuration.
     *
     * @param key Configuration key
     * @param enabled Enable status
     */
    protected void setEnabled(String key, boolean enabled) {
        put(key,enabled);
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
    protected boolean enabledDefault(APIConformance conformance) {
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
     * Checks {@link #defaultConformance} to determine if configuration is enabled.
     *
     * If no configuration is set, the default value is based on the conformance level information.
     *
     * @return {@code true} if conformance is enabled.
     */
    public boolean isEnabled() {
        if (serviceInfo.getMetadata().containsKey(metadataKey)) {
            Boolean enabled = get(defaultConformance.getKey(), Boolean.class);
            if (enabled != null) {
                return enabled;
            }
        }
        return enabledDefault(this.defaultConformance);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" ");
        sb.append(this.metadataKey);
        Object config = serviceInfo.getMetadata().get(metadataKey);
        if (config != null && config instanceof Map) {
            Map<String,Object> storage = (Map<String,Object>) config;
            sb.append("= [ ");
            storage.forEach((k,v) -> {
                sb.append(k);
                sb.append("=");
                sb.append(v);
                sb.append(" ");
            });
            sb.append("]");
        }
        return sb.toString();
    }
}

