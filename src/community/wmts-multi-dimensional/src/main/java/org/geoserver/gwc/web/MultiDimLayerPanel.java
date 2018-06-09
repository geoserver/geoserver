/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

/**
 * Configures expansion limits on a layer by layer basis. TODO: would be nice if this panel could go
 * into the Dimension tab, but there are no extension points there.
 */
public class MultiDimLayerPanel extends PublishedConfigurationPanel<LayerInfo> {

    public MultiDimLayerPanel(String id, IModel<? extends LayerInfo> model) {
        super(id, model);

        MapModel expandLimitDefaultModel =
                new MapModel(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        MultiDimensionalExtension.EXPAND_LIMIT_KEY);
        TextField<Integer> expandLimitDefault =
                new TextField<>("defaultExpandLimit", expandLimitDefaultModel, Integer.class);
        expandLimitDefault.add(RangeValidator.minimum(0));
        add(expandLimitDefault);

        MapModel expandLimitMaxModel =
                new MapModel(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        MultiDimensionalExtension.EXPAND_LIMIT_MAX_KEY);
        TextField<Integer> expandLimitMax =
                new TextField<>("maxExpandLimit", expandLimitMaxModel, Integer.class);
        expandLimitMax.add(RangeValidator.minimum(0));
        add(expandLimitMax);
    }
}
