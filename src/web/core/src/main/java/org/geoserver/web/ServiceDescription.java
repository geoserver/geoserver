/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.geotools.api.util.InternationalString;
import org.geotools.text.Text;

/** Description of a service acting as a model object to this panel's ListView. */
public class ServiceDescription implements Serializable, Comparable<ServiceDescription> {
    @Serial
    private static final long serialVersionUID = -7406652617944177247L;

    /**
     * Workspace prefix for virtual web service, may be null for global services.
     *
     * <p>Forced to lowercase for ease of comparison.
     */
    private final String workspace;

    /** Layer name for virtual web service, may be null for workspace or global services. */
    private final String layer;

    /** Service type, example {@code WMS}, {@code WFS}, {@code Features}, ... */
    private final String serviceType;

    /** Service title. */
    private final InternationalString title;

    /** Service description. */
    private final InternationalString description;

    /** Service availability; may be disabled or users may lack sufficient permissions. */
    private final boolean available;

    /** Service requires admin privileges */
    private final boolean admin;

    /**
     * if there are multiple ServiceDescriptions for a Category, then use this to determine the title of the category.
     * see getDescriptionPriority() *
     */
    private double descriptionPriority = 100.0;

    /**
     * User interface order to present common web services to highlight visual services such as WMS and WMTS ahead of
     * data access and processing.
     *
     * <p>Order has no real significance it is for display, see {@link #compareTo(ServiceDescription)}.
     */
    private static List<String> OGC_SERVICE_ORDER = Collections.unmodifiableList(
            Arrays.asList("CSW", "WMS", "WMTS", "WMS-C", "WFS", "WCS", "WPS", "Styles", "DGGS", "Experimental"));

    /** Service links. */
    Set<ServiceLinkDescription> links = new HashSet<>();

    /**
     * Service description based on service identifier, when no further details are available.
     *
     * @param serviceType Service identifier, example {@code WPS}
     */
    public ServiceDescription(String serviceType) {
        this(serviceType, null, null);
    }

    /**
     * Service description.
     *
     * @param serviceType Service identifier, example {@code WPS}
     * @param title Service title
     * @param description Service description
     */
    public ServiceDescription(String serviceType, InternationalString title, InternationalString description) {
        this(serviceType, title, description, true, false, null, null);
    }

    /**
     * Workspace service description.
     *
     * @param serviceType Service identifier, example {@code WPS}
     * @param title Service title
     * @param description Service description
     * @param workspace Workspace prefix, or {@code null} for global service
     */
    public ServiceDescription(
            String serviceType, InternationalString title, InternationalString description, String workspace) {
        this(serviceType, title, description, true, false, workspace, null);
    }

    /**
     * Layer or LayerGroup service description, with associated availability or admin restrictions.
     *
     * @param serviceType Service type, example {@code WPS}
     * @param title Service title, will default to serviceType if not provided
     * @param description Service description, will default to empty InternationalString if not provided
     * @param available {@code true} if service is available, {@code false} if service is disabled
     * @param admin {@code true} if service requires admin access (example REST services for configuration)
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
        this.serviceType = serviceType;
        this.workspace = workspace;
        this.layer = layer;
        this.available = available;
        this.admin = admin;

        if (title != null) {
            this.title = title;
        } else {
            this.title = Text.text(this.serviceType);
        }

        if (description != null) {
            this.description = description;
        } else {
            this.description = Text.text("");
        }
    }

    /**
     * If there are multiple ServiceDescriptions (i.e. WMS & OGCAPI-Features) for a Category, then we use the
     * descriptionPriority to determine which title to use.
     *
     * <p>In general, the WMS/WCS (etc) services should be high priority. Others, like OGCAPI should be lower.
     *
     * <p>The lower priority description is only used when the higher priority description isn't available. For example,
     * if OGCAPI-Features is turned on, but the main services (WFS) is turned off.
     *
     * <p>larger number = higher priority
     */
    public double getDescriptionPriority() {
        return descriptionPriority;
    }

    public void setDescriptionPriority(double descriptionPriority) {
        this.descriptionPriority = descriptionPriority;
    }

    /**
     * Service type, {@code WMS}, {@code WFS}, {@code Features}.
     *
     * @return service type, forced to lower case for ease of comparison.
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Service title as localized text.
     *
     * <p>If not provided service type is filled-in as a default, example {@code WMS}, {@code WFS}, {@code Features}.
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
                && serviceType.equals(that.serviceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, layer, serviceType);
    }

    /**
     * Compare common services based on {@link #OGC_SERVICE_ORDER} to highlight visual web services ahead of data access
     * and processing.
     *
     * <p>Comparison falls back string comparison to compare other services.
     *
     * <p>Order has no real significance it is for display.
     *
     * @param other the object to be compared.
     * @return service description order highlight visual web services ahead of data access and processing
     */
    @Override
    public int compareTo(ServiceDescription other) {

        int serviceOrder = OGC_SERVICE_ORDER.indexOf(this.serviceType);
        int serviceOrderOther = OGC_SERVICE_ORDER.indexOf(other.serviceType);

        if (serviceOrder == -1 && serviceOrderOther != -1) {
            return 1;
        } else if (serviceOrder != -1 && serviceOrderOther == -1) {
            return -1;
        } else if (serviceOrder != -1 && serviceOrderOther != -1) {
            return Integer.compare(serviceOrder, serviceOrderOther);
        } else {
            // fall back to string compare for non-ogc services
            return this.serviceType.compareTo(other.serviceType);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceDescription{");
        sb.append("service='").append(serviceType).append('\'');
        sb.append(", available=").append(available);
        sb.append(", workspace='").append(workspace).append('\'');
        sb.append(", layer='").append(layer).append('\'');
        sb.append(", links=").append(links.size());
        sb.append('}');
        return sb.toString();
    }
}
