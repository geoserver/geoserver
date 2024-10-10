/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish.dggs;

import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_OFFSET_KEY;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

/** Configures a layer DGGS related attributes */
public class DGGSConfigPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 6469105227923320272L;

    public DGGSConfigPanel(String id, IModel<LayerInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "resource.metadata");
        add(
                new TextField<>(
                        "resolutionOffset",
                        new MapModel<>(metadata, CONFIGURED_OFFSET_KEY),
                        Integer.class));
    }
}
