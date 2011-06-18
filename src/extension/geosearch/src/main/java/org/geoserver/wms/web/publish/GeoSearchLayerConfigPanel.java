/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.util.MapModel;

/**
 * Configures a {@link LayerInfo} geo-search related metadata
 */
@SuppressWarnings("serial")
public class GeoSearchLayerConfigPanel extends LayerConfigurationPanel{
    public GeoSearchLayerConfigPanel(String id, IModel model){
        super(id, model);

        add(new CheckBox("geosearch.enable", new MapModel(new PropertyModel(getDefaultModel(), "resource.metadata"), "indexingEnabled")));
    }
}
