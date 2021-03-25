/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;

public class KMLFormatLink extends CommonFormatLink {

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {
        ExternalLink kmlLink =
                new ExternalLink(
                        this.getComponentId(),
                        layer.getKmlLink(),
                        (new StringResourceModel(this.getTitleKey(), null, null)).getString());
        kmlLink.setVisible(layer.hasServiceSupport("WMS"));
        return kmlLink;
    }
}
