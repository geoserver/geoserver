/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geotools.util.Version;

/**
 * Default component for a {@link CapabilitiesHomePageLinkProvider} implementation to provide a list
 * of getcapabilities links discriminated by service name and version.
 *
 * @author Gabriel Roldan
 */
public class CapabilitiesHomePagePanel extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * A complete reference to a GetCapabilities or other service description document acting as the
     * model object to this panel's ListView.
     */
    public static class CapsInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        String service;

        Version version;

        String capsLink;

        public CapsInfo(String service, Version version, String capsLink) {
            this.service = service;
            this.version = version;
            this.capsLink = capsLink;
        }

        public String getService() {
            return service;
        }

        public Version getVersion() {
            return version;
        }

        public String getCapsLink() {
            return capsLink;
        }

        public boolean equals(Object o) {
            if (!(o instanceof CapsInfo)) {
                return false;
            }
            CapsInfo ci = (CapsInfo) o;
            return service.equals(ci.service)
                    && version.equals(ci.version)
                    && capsLink.equals(ci.capsLink);
        }

        @Override
        public int hashCode() {
            return Objects.hash(service, version, capsLink);
        }
    }

    /**
     * @param id this component's wicket id
     * @param capsLinks the list of getcapabilities link to create the component for
     */
    public CapabilitiesHomePagePanel(final String id, final List<CapsInfo> capsLinks) {

        super(id);

        final Map<String, List<CapsInfo>> byService = new HashMap<String, List<CapsInfo>>();
        for (CapsInfo c : capsLinks) {
            final String key =
                    c.getService().toLowerCase(); // to avoid problems with uppercase definitions
            List<CapsInfo> serviceLinks = byService.get(key);
            if (serviceLinks == null) {
                serviceLinks = new ArrayList<CapsInfo>();
                byService.put(key, serviceLinks);
            }
            serviceLinks.add(c);
        }

        ArrayList<String> services = new ArrayList<String>(byService.keySet());
        Collections.sort(services);

        ListView<String> view =
                new ListView<String>("services", services) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        final String serviceId = item.getModelObject();
                        item.add(new Label("service", serviceId.toUpperCase()));
                        item.add(
                                new ListView<CapsInfo>("versions", byService.get(serviceId)) {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void populateItem(ListItem<CapsInfo> item) {
                                        CapsInfo capsInfo = item.getModelObject();
                                        Version version = capsInfo.getVersion();
                                        String capsLink = capsInfo.getCapsLink();
                                        ExternalLink link = new ExternalLink("link", capsLink);
                                        item.add(link);

                                        link.add(new Label("version", version.toString()));
                                    }
                                });
                    }
                };

        add(view);
    }
}
