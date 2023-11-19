/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.mapml.MapMLConstants;

public class MapMLFormatLink extends CommonFormatLink {

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {
        ExternalLink olLink =
                new ExternalLink(
                        this.getComponentId(),
                        layer.getWmsLink() + "&format=" + MapMLConstants.MAPML_HTML_MIME_TYPE,
                        (new StringResourceModel(this.getTitleKey(), null, null)).getString());
        olLink.setVisible(layer.hasServiceSupport("WMS"));
        return olLink;
    }
}
