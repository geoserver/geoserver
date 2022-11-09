/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.geotools.text.Text;
import org.opengis.util.InternationalString;

/** Description of a service acting as a model object to this panel's ListView. */
public class ServiceDescription implements Serializable, Comparable<ServiceDescription> {
    private static final long serialVersionUID = -7406652617944177247L;

    /**
     * Workspace prefix for virtual web service, may be null for global services.
     *
     * <p>Forced to lowercase for ease of comparison.
     */
    private final String workspace;

    /** Layer name for virtual web service, may be null for workspace or global services. */
    private final String layer;

    /** Service type. */
    private final String service;

    /** Service title. */
    private final InternationalString title;

    /** Service description. */
    private final InternationalString description;

    /** Service availability; may be disabled or users may lack sufficient permissions. */
    private final boolean available;

    /** Service requires admin privileges */
    private final boolean admin;

    /**
     * Order to present common web services to highlight visual services such as WMS and WMTS ahead
     * of data acess and processing.
     */
    private static List<String> OGC_SERVICE_ORDER =
            Collections.unmodifiableList(Arrays.asList("wms", "wmts", "wfs", "wcs", "wps"));

    /** Service links. */
    Set<ServiceLinkDescription> links = new HashSet<>();

    /**
     * Service description based on service identifier, when no further details are available.
     *
     * @param service Service identifier, example {@code wps}
     */
    public ServiceDescription(String service) {
        this(service, null, null);
    }

    /**
     * Service description.
     *
     * @param service Service identifier, example {@code wps}
     * @param title Service title
     * @param description Service description
     */
    public ServiceDescription(
            String service, InternationalString title, InternationalString description) {
        this(service, title, description, true, false, null, null);
    }

    /**
     * Workspace service description.
     *
     * @param service Service identifier, example {@code wps}
     * @param title Service title
     * @param description Service description
     * @param workspace Workspace prefix, or {@code null} for global service
     */
    public ServiceDescription(
            String service,
            InternationalString title,
            InternationalString description,
            String workspace) {
        this(service, title, description, true, false, workspace, null);
    }

    /**
     * Layer or LayerGroup service description, with associated availability or admin restrictions.
     *
     * @param serviceType Service type, example {@code wps}
     * @param title Service title
     * @param description Service description
     * @param available {@code true} if service is available, {@code false} if service is disabled
     * @param admin {@code true} if service requires admin access (example REST services for
     *     configuration)
     * @param workspace Workspace prefix, or {@code null} for global service
     * @param layer Layer name, or LayerGroup name, or {@code null} for workspace or global service
     */
    public ServiceDescription(
            String serviceType,
            InternationalString title,
            InternationalString description,
            boolean available,
            boolean admin,
            String workspace,
            String layer) {
        this.service = serviceType.toLowerCase();
        this.workspace = workspace;
        this.layer = layer;
        this.available = available;
        this.admin = admin;

        if (title != null) {
            this.title = title;
        } else {
            this.title = Text.text(service.toUpperCase());
        }

        if (description != null) {
            this.description = description;
        } else {
            this.description = Text.text("");
        }
    }

    /**
     * Service type, example {@code wfs}, {@code wms}, {@code features}.
     *
     * @return service type, forced to lower case for ease of comparison.
     */
    public String getService() {
        return service;
    }

    /**
     * Service title as localized text.
     *
     * <p>If not provided uppercase service name, example {@code WMS}, {@code WFS}, {@code
     * OGCAPI-FEATURES}.
     *
     * @return service title
     */
    public InternationalString getTitle() {
        return title;
    }

    /**
     * Service description, if provided.
     *
     * @return service description, or {@code null} if not available.
     */
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Service availability; may be disabled or users may lack sufficient permissions.
     *
     * @return service availability
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Service requires admin role for use.
     *
     * @return service requires admin role for use.
     */
    public boolean isAdmin() {
        return admin;
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

    /**
     * Service link descriptions as matched by service type.
     *
     * @return service links
     */
    public Set<ServiceLinkDescription> getLinks() {
        return links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceDescription)) return false;
        ServiceDescription that = (ServiceDescription) o;
        return Objects.equals(workspace, that.workspace)
                && Objects.equals(layer, that.layer)
                && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, layer, service);
    }

    /**
     * Compare common services based on {@link #OGC_SERVICE_ORDER} to highlight visual web services
     * ahead of data access and processing.
     *
     * <p>Comparison falls back string comparison to compare other services.
     *
     * @param other the object to be compared.
     * @return service description order highlight visual * web services ahead of data access and
     *     processing
     */
    @Override
    public int compareTo(ServiceDescription other) {

        int serviceOrder = OGC_SERVICE_ORDER.indexOf(this.service);
        int serviceOrderOther = OGC_SERVICE_ORDER.indexOf(other.service);

        if (serviceOrder == -1 && serviceOrderOther != -1) {
            return 1;
        } else if (serviceOrder != -1 && serviceOrderOther == -1) {
            return -1;
        } else if (serviceOrder != -1 && serviceOrderOther != -1) {
            return Integer.compare(serviceOrder, serviceOrderOther);
        } else {
            // fall back to string compare for non-ogc services
            return this.service.compareTo(other.service);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceDescription{");
        sb.append("service='").append(service).append('\'');
        sb.append(", available=").append(available);
        sb.append(", workspace='").append(workspace).append('\'');
        sb.append(", layer='").append(layer).append('\'');
        sb.append(", links=").append(links.size());
        sb.append('}');
        return sb.toString();
    }
}
