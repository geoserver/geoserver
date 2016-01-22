/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.web;

import static org.geoserver.geosearch.rest.Properties.INDEXING_ENABLED;
import static org.geoserver.geosearch.rest.Properties.LAST_MODIFIED;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.util.MapModel;

/**
 * Configures a {@link LayerGroupInfo} geo-search related metadata
 */
public class GeoSearchResourceConfigPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 5739568775378997529L;

    public GeoSearchResourceConfigPanel(String id, IModel<? extends ResourceInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadataModel;
        metadataModel = new PropertyModel<MetadataMap>(model, "metadata");

        MapModel<Long> lastModModel = new MapModel<Long>(metadataModel, LAST_MODIFIED);
        lastModModel.setObject(Long.valueOf(System.currentTimeMillis()));

        MapModel<Boolean> enableIndexModel = new MapModel<Boolean>(metadataModel, INDEXING_ENABLED);
        CheckBox enable = new CheckBox("geosearch.enable", enableIndexModel);
        add(enable);
    }
}
