/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geotools.util.logging.Logging;

/**
 * Configures {@link LayerInfo} WMS specific attributes.
 *
 * <ul>
 *   <li>queryableEnabled
 *   <li>wmsPath
 * </ul>
 */
public class WMSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(WMSLayerConfig.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    static final Logger LOGGER = Logging.getLogger(WMSLayerConfig.class);

    @Serial
    private static final long serialVersionUID = -2895136226805357532L;

    public WMSLayerConfig(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        add(new CheckBox("queryableEnabled", new PropertyModel<>(layerModel, "queryable")));

        add(new TextField<>("wmsPath", new PropertyModel<>(layerModel, "path")));
    }
}
