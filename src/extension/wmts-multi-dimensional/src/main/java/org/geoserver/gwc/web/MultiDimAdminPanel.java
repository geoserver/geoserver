/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;

/** Configures expansion limits on a layer by layer basis. */
public class MultiDimAdminPanel extends AdminPagePanel {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

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

    public MultiDimAdminPanel(String id, IModel<? extends ServiceInfo> model) {
        super(id, model);

        MapModel<Integer> expandLimitDefaultModel =
                new MapModel<>(new PropertyModel<>(model, "metadata"), MultiDimensionalExtension.EXPAND_LIMIT_KEY);
        TextField<Integer> expandLimitDefault =
                new TextField<>("defaultExpandLimit", expandLimitDefaultModel, Integer.class);
        expandLimitDefault.add(RangeValidator.minimum(0));
        add(expandLimitDefault);

        MapModel<Integer> expandLimitMaxModel =
                new MapModel<>(new PropertyModel<>(model, "metadata"), MultiDimensionalExtension.EXPAND_LIMIT_MAX_KEY);
        TextField<Integer> expandLimitMax = new TextField<>("maxExpandLimit", expandLimitMaxModel, Integer.class);
        expandLimitMax.add(RangeValidator.minimum(0));
        add(expandLimitMax);
    }
}
