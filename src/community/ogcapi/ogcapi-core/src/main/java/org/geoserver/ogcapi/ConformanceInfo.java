package org.geoserver.ogcapi;

import org.geoserver.config.ServiceInfo;

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
     * Indicates if this service module is enabled.
     *
     * The default implementation metadata map for a non-false value.
     */
    public boolean isEnabled() {
        if (!serviceInfo.isEnabled()) {
            return false;
        }

        if (!serviceInfo.getMetadata().containsKey(conformance) || serviceInfo.getMetadata().get(conformance) == null) {
            return conformance.getStatus() == APIConformance.Status.APPROVED;
        }
        Object conformanceInfo = serviceInfo.getMetadata().get(conformance);

        return conformanceInfo != null && !Boolean.FALSE.equals(conformanceInfo);
    }

    @Override
    public String toString() {
        return "ConformanceInfo {" +
                "conformance='" + conformance + '\'' +
                '}';
    }
}

