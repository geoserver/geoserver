/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.List;
import org.geotools.util.Version;

/**
 * A service descriptor which provides metadata, primarily service {@code id}, and {@code version} allowing
 * {@code Dispatcher} to execute requests to the correct operation.
 *
 * <p>A Service is identified by an {@link #id}, {@link #version} pair. Two service descriptors are considered equal if
 * they have the same {@code id}, and {@code version}.
 *
 * <p>The underlying service implementation is a plain old java object, available via {@link #service}.
 *
 * <p>The {@code id} is treated as a service type by ServiceDescriptor for presentation, and by ServiceResourceProvider
 * for service enablement.
 *
 * <p>Services may be composed of several {@link #operations}.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public final class Service {

    /**
     * Service type identifying the service.
     *
     * <p>This is used by the dispatcher for open web services, where several services may implement different versions
     * of a protocol.
     *
     * <p>This is used to obtain a service info configuring the service.
     */
    final String id;

    /**
     * Namespace for the web service specification.
     *
     * <p>As an example the {@code wfs} standard {@code http://www.opengis.net/wfs}, and
     * {@code http://www.opengis.net/wfs/2.0} depending on version.
     */
    final String namespace;

    /**
     * The service implementation.
     *
     * <p>The service is configured using matching ServiceInfo matching, implicitly matching {@link #id}
     */
    final Object service;

    /** The service version */
    final Version version;

    /** List of operations provided by the service */
    final List<String> operations;

    /** Optional capabilities backlink in case the service is not a traditional OWS service */
    String customCapabilitiesLink;

    /**
     * Creates a new global (no workspace) service descriptor.
     *
     * @param id service type used to identify the service.
     * @param service The object implementing the service.
     * @param version The version of the service.
     */
    public Service(String id, Object service, Version version, List<String> operations) {
        this(id, null, service, version, operations);
    }

    /**
     * Creates a new service descriptor.
     *
     * @param id service type used to identify the service.
     * @param namespace Optional XML namespace of the service specification.
     * @param service The object implementing the service.
     * @param version The version of the service.
     * @param operations The list of operations the service provides
     */
    public Service(String id, String namespace, Object service, Version version, List<String> operations) {
        this.id = id;
        this.service = service;
        this.version = version;
        this.operations = operations;
        this.namespace = namespace;
        if (id == null) {
            throw new NullPointerException("id");
        }
    }

    /**
     * Service type used to identify the service.
     *
     * <p>This is required to tbe the service type, example {@code WMS}, {@code WFS}, {@code Features} allowing
     * ServiceDescription and ServiceResourceProvider.getServicesForResource(layer) to manage user interaction with web
     * services.
     *
     * @return service type used to identify the service
     */
    public String getId() {
        return id;
    }

    /**
     * Namespace of the web service specification.
     *
     * @return XML namespace of the XML web service.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Service object.
     *
     * @return service object
     */
    public Object getService() {
        return service;
    }

    public Version getVersion() {
        return version;
    }

    /**
     * List of operations provided by the service.
     *
     * @return operations provided by the service.
     */
    public List<String> getOperations() {
        return operations;
    }

    /**
     * Custom landing page, or GetCapabilities document, for the web service.
     *
     * @return GetCapabilities document or landing page for the web service.
     */
    public String getCustomCapabilitiesLink() {
        return customCapabilitiesLink;
    }

    /**
     * Custom landing page, or GetCapabilities document, for the web service.
     *
     * @param customCapabilitiesLink GetCapabilities document or landing page for the web service.
     */
    public void setCustomCapabilitiesLink(String customCapabilitiesLink) {
        this.customCapabilitiesLink = customCapabilitiesLink;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Service)) {
            return false;
        }

        Service other = (Service) obj;

        if (!id.equals(other.id)) {
            return false;
        }

        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else {
            if (!version.equals(other.version)) {
                return false;
            }
        }

        return operations.equals(other.operations);
    }

    @Override
    public int hashCode() {

        int result = id.hashCode();

        if (version != null) {
            result = (result * 17) + version.hashCode();
        }

        result = (result * 17) + operations.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "Service( " + id + ", " + version + " )";
    }
}
