/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.Objects;
import org.geotools.util.Version;

/**
 * A complete reference to a GetCapabilities, REST API, or other service description document.
 *
 * <p>ServiceLinkDescription is associated with a {@link ServiceDescription} by service type {@link
 * #getServiceType()}.
 *
 * <p>This description is a model object for the {@link ServicesPanel}.
 */
public class ServiceLinkDescription implements Serializable, Comparable<ServiceLinkDescription> {
    private static final long serialVersionUID = -5600492358023139816L;

    /** Service type, example {@code WMS}, {@code WFS}, {@code Features}, ... */
    private final String serviceType;

    /** Protocol */
    private final String protocol;

    /** Service version */
    private final Version version;

    /** Service link (example GetCapabilities) */
    private final String link;

    /** Workspace prefix for virtual web service, may be null for global services. */
    private final String workspace;

    /** Layer name for virtual web service, may be null for workspace or global services. */
    private final String layer;

    public ServiceLinkDescription(
            String serviceType, Version version, String link, String workspace, String layer) {
        this(serviceType, version, link, workspace, layer, null);
    }

    public ServiceLinkDescription(
            String serviceType,
            Version version,
            String link,
            String workspace,
            String layer,
            String protocol) {
        this.serviceType = serviceType;
        this.version = version;
        this.link = link;
        this.workspace = workspace;
        this.layer = layer;
        this.protocol = protocol != null ? protocol : this.serviceType;
    }

    /**
     * Service type, example {@code WFS}, {@code WMS}, {@code Features}.
     *
     * <p>Service type is internal to GeoServer codebase, while the title is used to identify the
     * service to users.
     *
     * <p>A given internal service type may support several external protocols and versions (see
     * below).
     *
     * @return service name, forced to lower case for ease of comparison.
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Web service protocol, example {@code WMTS}, {@code TMS}, {@code WMS-C}.
     *
     * <p>Service protocol is publicly shown to users when describing a service end-point.
     *
     * @return service protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Service version.
     *
     * @return service version.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Service link, for OGC OpenWebServices this a GetCapabilities document.
     *
     * @return service link, if {@code null} open web service capabilities document assumed
     */
    public String getLink() {
        return link;
    }

    /**
     * Service workspace name, or {@code null} for global services.
     *
     * @return service workspace, or {@code null} for global services.
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Layer name, or {@code null} for workspace or global services.
     *
     * @return layer name, or {@code null} for workspace or global services.
     */
    public String getLayer() {
        return layer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceLinkDescription)) return false;
        ServiceLinkDescription that = (ServiceLinkDescription) o;
        return Objects.equals(workspace, that.workspace)
                && Objects.equals(layer, that.layer)
                && serviceType.equals(that.serviceType)
                && version.equals(that.version)
                && Objects.equals(link, that.link)
                && protocol.equals(that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, layer, serviceType, version, link, protocol);
    }

    @Override
    public int compareTo(ServiceLinkDescription o) {
        int compareProtocol = this.protocol.compareTo(o.protocol);
        int compareVersion = -this.version.compareTo(o.getVersion());
        return compareProtocol != 0 ? compareProtocol : compareVersion;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceLinkDescription{");
        sb.append("service='").append(serviceType).append('\'');
        sb.append(", version=").append(version);
        sb.append(", protocol='").append(protocol).append('\'');
        sb.append(", workspace='").append(workspace).append('\'');
        sb.append(", layer='").append(layer).append('\'');
        sb.append(", link='").append(link).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
