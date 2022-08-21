/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
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

        /** Service name. */
        String service;

        /** Service version */
        Version version;

        /** GetCapabilities service description */
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

        @Override
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
        capsLinks.sort(
                new Comparator<CapsInfo>() {
                    @Override
                    public int compare(CapsInfo o1, CapsInfo o2) {
                        int serviceOrder =
                                o1.getService()
                                        .toUpperCase()
                                        .compareTo(o2.getService().toUpperCase());
                        int versionOrder = -o1.version.compareTo(o2.getVersion());

                        return serviceOrder != 0 ? serviceOrder : versionOrder;
                    }
                });

        ListView<CapsInfo> view =
                new ListView<CapsInfo>("services", capsLinks) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<CapsInfo> captItem) {
                        CapsInfo capsInfo = captItem.getModelObject();

                        Version version = capsInfo.getVersion();
                        String capsLink = capsInfo.getCapsLink();
                        ExternalLink link = new ExternalLink("link", capsLink);

                        link.add(new Label("service", capsInfo.getService().toUpperCase()));
                        link.add(new Label("version", version.toString()));

                        captItem.add(link);
                    }
                };
        add(view);
    }
}
