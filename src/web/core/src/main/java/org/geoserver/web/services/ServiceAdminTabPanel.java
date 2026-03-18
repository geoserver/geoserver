/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.geoserver.web.util.SerializableConsumer;

/** Panel used for service configuration, built around an initial page, with AdminPagePanel extensions. */
public class ServiceAdminTabPanel extends AdminPagePanel {
    @Serial
    private static final long serialVersionUID = -1;

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(ServiceAdminTabPanel.class);

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

    public ServiceAdminTabPanel(
            String panelId,
            IModel<?> infoModel,
            AdminPagePanel initialPanel,
            List<AdminPagePanelInfo> extensionPanels,
            List<SerializableConsumer<Void>> onSubmitHooks) {
        super(panelId, infoModel);

        if (initialPanel != null) {
            add(initialPanel);
        } else {
            Label placeHolder = new Label("initial");
            placeHolder.setVisible(false);
            add(placeHolder);
        }
        ListView extensionPanelView =
                new AdminPagePanelInfoListView("extensions", extensionPanels, infoModel, onSubmitHooks);
        extensionPanelView.setReuseItems(true);
        add(extensionPanelView);
    }
}
