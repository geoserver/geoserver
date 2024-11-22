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

    final APIConformance conformance;
    final S serviceInfo;

    /**
     * Enable and configure service functionality by conformance class.
     *
     * @param serviceInfo
     * @param conformance
     */
    public ConformanceInfo(APIConformance conformance, S serviceInfo){
        if (conformance == null){
            throw new NullPointerException("conformance is null");
        }
        this.conformance = conformance;
        if (serviceInfo == null){
            throw new NullPointerException("serviceInfo is null");
        }
        this.serviceInfo = serviceInfo;
    }

    /**
     * Conformance class identifier.
     *
     * @return conformance class identifier.
     */
    public String getId() {
        return conformance.getId();
    }

    /**
     * Conformance class declaration.
     *
     * @return conformance class declaration.
     */
    public APIConformance getConformance() {
        return conformance;
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
            HashMap<String, Object> configuration = serviceInfo.getMetadata().get(conformance.getId(),HashMap.class);
            if (configuration == null) {
                configuration = new HashMap<>();
                serviceInfo.getMetadata().put(conformance.getId(), configuration);
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
     * Set conformance metadata configuration.
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
        // default enabled based on conformance level
        if (serviceInfo.isCiteCompliant()) {
            return conformance.getLevel().isEndorsed() && conformance.getLevel().isStable();
        }
        else {
            return conformance.getLevel().isStable();
        }
    }
    /**
     * Checks {@code enabled} value to determine if conformance has been enabled by user or by default.
     * <p>
     * If not set, value will be based on {@link #getId()} level information, and serviceInfo settings.
     *
     * @return {@code true} if conformance is enabled by user, or by default.
     */
    public boolean isEnabled() {
        if (serviceInfo.getMetadata().containsKey(getConformance().getId())) {
            Boolean enabled = get(ENABLED, Boolean.class);
            if (enabled != null) {
                return enabled;
            }
        }
        return enabledDefault(getConformance());
    }

    /**
     * Enable or disable conformance for {@link #getId()}.
     *
     * @param enabled Enable conformance
     */
    public void setEnabled(boolean enabled) {
        setEnabled(ENABLED, enabled);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" ");
        sb.append(conformance.getId());
        Object config = serviceInfo.getMetadata().get(conformance.getId());
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

