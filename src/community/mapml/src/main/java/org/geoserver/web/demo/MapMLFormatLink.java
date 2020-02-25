/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;

public class MapMLFormatLink extends CommonFormatLink {

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {
        ExternalLink mapmlLink =
                new ExternalLink(
                        this.getComponentId(),
                        layer.getBaseURL("", true) + "mapml/" + layer.getName() + "/osmtile/",
                        (new StringResourceModel(this.getTitleKey(), (Component) null, null))
                                .getString());
        mapmlLink.setVisible(layer.hasServiceSupport("WMS"));
        return mapmlLink;
    }
}
