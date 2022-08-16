/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geotools.text.Text;
import org.geotools.util.Version;
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
        final String workspace;

        /** Layer name for virtual web service, may be null for workspace or global services. */
        final String layer;

        /** Service name. */
        final String service;

        /** Service title. */
        final InternationalString title;

        /** Service description. */
        final InternationalString description;

        /** Service availability; may be disabled or users may lack sufficient permissions. */
        final boolean available;

        /** Service links. */
        SortedSet<ServiceLinkDescription> links = new TreeSet<>();

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
        public SortedSet<ServiceLinkDescription> getLinks() {
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
            return this.service.compareTo(o.service);
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

        /** Service version */
        private final Version version;

        /** Service link (example GetCapabilities) */
        private final String link;

        /** Workspace prefix for virtual web service, may be null for global services. */
        private final String workspace;

        /** Layer name for virtual web service, may be null for workspace or global services. */
        private final String layer;

        public ServiceLinkDescription(String service, Version version, String link) {
            this(service, version, link, null);
        }

        public ServiceLinkDescription(
                String service, Version version, String link, String workspace) {
            this(service, version, link, workspace, null);
        }

        public ServiceLinkDescription(
                String service, Version version, String link, String workspace, String layer) {
            this.service = service.toLowerCase();
            this.version = version;
            this.link = link;
            this.workspace = workspace;
            this.layer = layer;
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
                    && Objects.equals(link, that.link);
        }

        @Override
        public int hashCode() {
            return Objects.hash(workspace, layer, service, version, link);
        }

        @Override
        public int compareTo(ServiceLinkDescription o) {
            int compareService = this.service.compareTo(o.service);
            int compareVersion = -this.version.compareTo(o.getVersion());
            return compareService != 0 ? compareService : compareVersion;
        }
    }

    public ServicesPanel(
            final String id,
            final List<ServiceDescription> services,
            List<ServiceLinkDescription> links) {
        super(id);

        final SortedSet<ServiceDescription> serviceSet = processServiceLinks(services, links);

        class ServiceLinkListView extends ListView<ServiceLinkDescription> {
            public ServiceLinkListView(String id, List<ServiceLinkDescription> list) {
                super(id, list);
            }

            @Override
            protected void populateItem(ListItem<ServiceLinkDescription> listItem) {
                ServiceLinkDescription link = listItem.getModelObject();

                listItem.add(new Label("serviceName", link.getService().toUpperCase()));

                ExternalLink externalLink = new ExternalLink("serviceLink", link.getLink());
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
                // Collections.sort(links);

                listItem.add(new ServiceLinkListView("links", links));
            }
        }

        List<ServiceDescription> serviceList = new ArrayList<>(serviceSet);
        // Collections.sort(serviceList);

        add(new ServiceListView("serviceDescriptions", serviceList));
    }

    /**
     * Assemble service and link descriptions into order for display.
     *
     * @param services service descriptions
     * @param links service link descriptions
     * @return map of service descriptions to link descriptions
     */
    SortedSet<ServiceDescription> processServiceLinks(
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
                // ignore
                //                ServiceDescription service = new ServiceDescription(serviceName);
                //                serviceMap.put(serviceName, service);
                //                service.getLinks().add(link);
            }
        }
        return new TreeSet<>(serviceMap.values());
    }
}
