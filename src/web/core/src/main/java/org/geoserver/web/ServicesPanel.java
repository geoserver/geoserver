/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geotools.util.logging.Logging;

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

    public ServicesPanel(
            final String id,
            final List<ServiceDescription> services,
            List<ServiceLinkDescription> links,
            final boolean admin) {
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
                boolean enabled = service.isAdmin() ? admin : true;

                listItem.add(
                        new Label("title", service.getTitle().toString(locale))
                                .setEnabled(enabled));
                listItem.add(
                        new Label("description", service.getDescription().toString(locale))
                                .setEnabled(enabled));

                List<ServiceLinkDescription> links = new ArrayList<>();
                if (enabled) {
                    links.addAll(service.getLinks());
                    Collections.sort(links);
                }
                listItem.add(new ServiceLinkListView("links", links));

                listItem.setVisible(service.isAvailable() && enabled);
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
