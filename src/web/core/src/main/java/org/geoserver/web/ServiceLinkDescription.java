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
 * <p>This description is a model object for the {@link ServicesPanel}.
 */
public class ServiceLinkDescription implements Serializable, Comparable<ServiceLinkDescription> {
    private static final long serialVersionUID = -5600492358023139816L;

    /** Service name. */
    private final String service;

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
            String service, Version version, String link, String workspace, String layer) {
        this(service, version, link, workspace, layer, null);
    }

    public ServiceLinkDescription(
            String service,
            Version version,
            String link,
            String workspace,
            String layer,
            String protocol) {
        this.service = service.toLowerCase();
        this.version = version;
        this.link = link;
        this.workspace = workspace;
        this.layer = layer;
        this.protocol = protocol != null ? protocol : service.toUpperCase();
    }

    /**
     * Service name, example wfs, wms, ogcapi-features.
     *
     * <p>A given service may support several protocols and versions (see below).
     *
     * @return service name, forced to lower case for ease of comparison.
     */
    public String getService() {
        return service;
    }

    /**
     * Web service protocol, example "wms", "wmc-c", "wmts", "rest".
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
     * Service link, often a GetCapabilities document.
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
                && service.equals(that.service)
                && version.equals(that.version)
                && Objects.equals(link, that.link)
                && protocol.equals(that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, layer, service, version, link, protocol);
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
        sb.append("service='").append(service).append('\'');
        sb.append(", version=").append(version);
        sb.append(", protocol='").append(protocol).append('\'');
        sb.append(", workspace='").append(workspace).append('\'');
        sb.append(", layer='").append(layer).append('\'');
        sb.append(", link='").append(link).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
