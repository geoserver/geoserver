/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geotools.text.Text;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * Component to list services and their connection details (such as GetCapabilities URL).
 *
 * <p>The panel displays a sorted list of ServiceDescription items to group ServiceLinkDescription
 * items.
 *
 * @author Jody Garnett
 */
public class ServicesPanel extends Panel {
    private static final long serialVersionUID = 5536322717819915862L;

    /** Description of a service acting as a model object to this panel's ListView. */
    public static class ServiceDescription implements Serializable, Comparable<ServiceDescription> {
        private static final long serialVersionUID = -7406652617944177247L;

        /**
         * Workspace prefix for virtual web service, may be null for global services.
         *
         * <p>Forced to lowercase for ease of comparison.
         */
        private final String workspace;

        /** Layer name for virtual web service, may be null for workspace or global services. */
        private final String layer;

        /** Service name. */
        private final String service;

        /** Service title. */
        private final InternationalString title;

        /** Service description. */
        private final InternationalString description;

        /** Service availability; may be disabled or users may lack sufficient permissions. */
        private final boolean available;

        private static List<String> ORDER =
                new ArrayList<>(Arrays.asList("wms", "wmts", "wfs", "wcs", "wps", "rest"));

        /** Service links. */
        Set<ServiceLinkDescription> links = new HashSet<>();

        public ServiceDescription(String service) {
            this(service, null, null);
        }

        public ServiceDescription(
                String service, InternationalString title, InternationalString description) {
            this(service, title, description, true, null, null);
        }

        public ServiceDescription(
                String service,
                InternationalString title,
                InternationalString description,
                String workspace) {
            this(service, title, description, true, workspace, null);
        }

        public ServiceDescription(
                String service,
                InternationalString title,
                InternationalString description,
                boolean available,
                String workspace,
                String layer) {
            this.service = service.toLowerCase();
            this.workspace = workspace;
            this.layer = layer;
            this.available = available;

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
         * Service name, example wfs, wms, ogcapi-features.
         *
         * @return service name, forced to lower case for ease of comparison.
         */
        public String getService() {
            return service;
        }

        /**
         * Service title as localized text.
         *
         * <p>If not provided uppercase service name, example WMS, WFS, OGCAPI-FEATURES.
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
         * Service links.
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

        @Override
        public int compareTo(ServiceDescription o) {
            if (ORDER.indexOf(this.service) == -1) {
                ORDER.add(this.service);
            }
            if (ORDER.indexOf(o.service) == -1) {
                ORDER.add(o.service);
            }
            return Integer.compare(ORDER.indexOf(this.service), ORDER.indexOf(o.service));
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ServiceDescription{");
            sb.append("service='").append(service).append('\'');
            sb.append(", available=").append(available);
            sb.append(", workspace='").append(workspace).append('\'');
            sb.append(", layer='").append(layer).append('\'');
            sb.append(", links=").append(links.size());
            sb.append('}');
            return sb.toString();
        }
    }
    /**
     * A complete reference to a GetCapabilities or other service description document acting as the
     * model object to this panel's ListView.
     */
    public static class ServiceLinkDescription
            implements Serializable, Comparable<ServiceLinkDescription> {
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
            final StringBuffer sb = new StringBuffer("ServiceLinkDescription{");
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

    public ServicesPanel(
            final String id,
            final List<ServiceDescription> services,
            List<ServiceLinkDescription> links) {
        super(id);

        final Set<ServiceDescription> serviceSet = processServiceLinks(services, links);

        class ServiceLinkListView extends ListView<ServiceLinkDescription> {
            public ServiceLinkListView(String id, List<ServiceLinkDescription> list) {
                super(id, list);
            }

            @Override
            protected void populateItem(ListItem<ServiceLinkDescription> listItem) {
                ServiceLinkDescription link = listItem.getModelObject();

                ExternalLink externalLink = new ExternalLink("serviceLink", link.getLink());

                externalLink.add(new Label("serviceProtocol", link.getProtocol()));
                externalLink.add(new Label("serviceVersion", link.getVersion().toString()));

                listItem.add(externalLink);
            }
        }

        class ServiceListView extends ListView<ServiceDescription> {
            public ServiceListView(String id, List<ServiceDescription> list) {
                super(id, list);
            }

            @Override
            protected void populateItem(ListItem<ServiceDescription> listItem) {
                ServiceDescription service = listItem.getModelObject();
                Locale locale = getLocale();

                listItem.add(new Label("title", service.getTitle().toString(locale)));
                listItem.add(new Label("description", service.getDescription().toString(locale)));

                List<ServiceLinkDescription> links = new ArrayList<>(service.getLinks());
                Collections.sort(links);

                listItem.add(new ServiceLinkListView("links", links));

                listItem.setVisible(service.isAvailable());
            }
        }

        List<ServiceDescription> serviceList = new ArrayList<>(serviceSet);
        Collections.sort(serviceList);

        add(new ServiceListView("serviceDescriptions", serviceList));
    }

    /**
     * Assemble service and link descriptions into order for display.
     *
     * @param services service descriptions
     * @param links service link descriptions
     * @return map of service descriptions to link descriptions
     */
    Set<ServiceDescription> processServiceLinks(
            final List<ServiceDescription> services, List<ServiceLinkDescription> links) {
        final Map<String, ServiceDescription> serviceMap = new HashMap<>();

        for (ServiceDescription service : services) {
            String serviceName = service.getService();
            serviceMap.put(serviceName, service);
            service.getLinks().clear();
        }
        for (ServiceLinkDescription link : links) {
            String serviceName = link.getService();
            if (serviceMap.containsKey(serviceName)) {
                ServiceDescription service = serviceMap.get(serviceName);
                service.getLinks().add(link);
            } else {
                // something is inconsistent
                Logger LOGGER = Logging.getLogger(ServicesPanel.class);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Service '"
                                    + serviceName
                                    + "' created without description to display "
                                    + link);
                }
                ServiceDescription service = new ServiceDescription(serviceName);
                serviceMap.put(serviceName, service);
                service.getLinks().add(link);
            }
        }
        return new HashSet<>(serviceMap.values());
    }
}
