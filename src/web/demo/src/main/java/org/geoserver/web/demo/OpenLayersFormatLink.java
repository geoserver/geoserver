/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;

public class OpenLayersFormatLink extends CommonFormatLink {

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {

        ExternalLink olLink =
                new ExternalLink(
                        this.getComponentId(),
                        layer.getWmsLink() + "&format=application/openlayers",
                        (new StringResourceModel(this.getTitleKey(), (Component) null, null))
                                .getString());
        olLink.setVisible(layer.hasServiceSupport("WMS"));
        return olLink;
    }
}
