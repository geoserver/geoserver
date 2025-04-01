/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.util.InternationalString;
import org.geotools.text.Text;
import org.geotools.util.Version;

/** Describe REST services, which requires admin access to be listed in the user interface. */
public class RESTServiceDescriptionProvider extends org.geoserver.web.ServiceDescriptionProvider {

    /** Service type to cross-link between service description and service link description. */
    public static final String SERVICE_TYPE = "REST";

    public RESTServiceDescriptionProvider() {
        super(SERVICE_TYPE);
    }

    @Override
    public List<ServiceDescription> getServices(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServiceDescription> descriptions = new ArrayList<>();

        Localizer localizer = GeoServerApplication.get().getResourceSettings().getLocalizer();

        InternationalString title;
        if (workspaceInfo != null) {
            HashMap<String, String> params = new HashMap<>();
            params.put("workspace", workspaceInfo.getName());
            title = Text.text(
                    localizer.getString("RESTServiceDescriptionProvider.workspace", null, new Model<>(params)));
        } else {
            title = Text.text(localizer.getString("RESTServiceDescriptionProvider.title", null));
        }
        InternationalString description =
                Text.text(localizer.getString("RESTServiceDescriptionProvider.description", null));

        ServiceDescription restDescription = new ServiceDescription(
                SERVICE_TYPE,
                title,
                description,
                true,
                true,
                workspaceInfo != null ? workspaceInfo.getName() : null,
                layerInfo != null ? layerInfo.getName() : null);

        descriptions.add(restDescription);
        return descriptions;
    }

    @Override
    public List<ServiceLinkDescription> getServiceLinks(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceLinkDescription> links = new ArrayList<>();
        if (workspaceInfo == null) {
            links.add(new ServiceLinkDescription(SERVICE_TYPE, new Version("1.0.0"), "../rest", null, null, "REST"));
        } else {
            links.add(new ServiceLinkDescription(
                    SERVICE_TYPE,
                    new Version("1.0.0"),
                    "../rest/workspaces/" + workspaceInfo.getName(),
                    workspaceInfo != null ? workspaceInfo.getName() : null,
                    null,
                    "REST"));
        }
        return links;
    }
}
