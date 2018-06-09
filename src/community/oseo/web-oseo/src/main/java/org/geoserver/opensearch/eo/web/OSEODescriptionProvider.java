/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.geoserver.web.CapabilitiesHomePageLinkProvider;
import org.geoserver.web.CapabilitiesHomePagePanel;
import org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo;
import org.geotools.util.Version;

/** Contributes a link to the OSEO OSDD document */
public class OSEODescriptionProvider implements CapabilitiesHomePageLinkProvider {

    /** @see org.geoserver.web.CapabilitiesHomePageLinkProvider#getCapabilitiesComponent */
    public Component getCapabilitiesComponent(final String id) {
        List<CapsInfo> serviceInfoLinks = new ArrayList<CapabilitiesHomePagePanel.CapsInfo>();
        String capsLink = "../oseo/description";
        CapsInfo ci = new CapsInfo("oseo", new Version("1.0"), capsLink);
        serviceInfoLinks.add(ci);
        return new CapabilitiesHomePagePanel(id, serviceInfoLinks);
    }
}
