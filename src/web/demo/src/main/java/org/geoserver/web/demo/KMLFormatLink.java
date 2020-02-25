/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;

public class KMLFormatLink extends CommonFormatLink {

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {
        ExternalLink kmlLink =
                new ExternalLink(
                        this.getComponentId(),
                        layer.getWmsLink() + "/kml?layers=" + layer.getName(),
                        (new StringResourceModel(this.getTitleKey(), (Component) null, null))
                                .getString());
        kmlLink.setVisible(layer.hasServiceSupport("WMS"));
        return kmlLink;
    }
}
