/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.model.StringResourceModel;
import org.geoserver.web.PreviewLink;

public class OpenLayersFormatLink extends CommonFormatLink {

    @Override
    public PreviewLink getFormatLink(PreviewLayer layer) {
        if (!layer.hasServiceSupport("WMS")) return null;
        String label = new StringResourceModel(this.getTitleKey(), null, null).getString();
        return new PreviewLink(label, layer.getWmsLink() + "&format=application/openlayers", label);
    }
}
