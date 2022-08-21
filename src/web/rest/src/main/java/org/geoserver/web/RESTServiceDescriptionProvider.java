/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Localizer;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.text.Text;
import org.geotools.util.Version;
import org.opengis.util.InternationalString;

public class RESTServiceDescriptionProvider extends ServiceDescriptionProvider {

    @Override
    public List<ServicesPanel.ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServicesPanel.ServiceDescription> descriptions = new ArrayList<>();

        InternationalString title = Text.text("REST");

        Localizer localizer = GeoServerApplication.get().getResourceSettings().getLocalizer();
        InternationalString description =
                Text.text(localizer.getString("RESTServiceDescriptionProvider.description", null));

        ServicesPanel.ServiceDescription restDescription =
                new ServicesPanel.ServiceDescription(
                        "rest",
                        title,
                        description,
                        workspaceInfo == null && layerInfo == null,
                        null,
                        null);

        descriptions.add(restDescription);
        return descriptions;
    }

    @Override
    public List<ServicesPanel.ServiceLinkDescription> getServiceLinks(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServicesPanel.ServiceLinkDescription> links = new ArrayList<>();
        links.add(
                new ServicesPanel.ServiceLinkDescription(
                        "rest", new Version("1.0.0"), "../rest", null, null, "REST"));
        return links;
    }
}
