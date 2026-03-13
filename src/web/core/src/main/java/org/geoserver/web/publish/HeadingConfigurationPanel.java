/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.Localizer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;

/** Adds a visual heading to publishing page. */
public class HeadingConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(HeadingConfigurationPanel.class);

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

    @Serial
    private static final long serialVersionUID = -907171664833447963L;

    public HeadingConfigurationPanel(String id, IModel<LayerInfo> model, PublishedConfigurationPanelInfo panelInfo) {
        super(id, model);

        Label title;
        if (panelInfo != null && panelInfo.getTitleKey() != null) {
            title = new Label("title", new StringResourceModel(panelInfo.getTitleKey(), null, null));
        } else {
            title = new Label("title", "placeholder");
            title.setVisible(false);
        }
        add(title);

        Label description;
        if (panelInfo != null && panelInfo.getDescriptionKey() != null) {
            description = new Label("description", new StringResourceModel(panelInfo.getDescriptionKey(), null, null));
        } else {
            description = new Label("description", "placeholder");
            description.setVisible(false);
        }
        add(description);
    }

    Label createLabel(String id, String key) {
        Localizer localizer = GeoServerApplication.get().getResourceSettings().getLocalizer();
        String text = null;
        try {
            text = localizer.getString(key, this);
        } catch (java.util.MissingResourceException e) {
            // ignore missing key, just don't show the label
        }
        if (!Strings.isEmpty(text)) {
            return new Label(id, text);
        } else {
            return new Label(id, "{" + key + "}");
        }
    }
}
