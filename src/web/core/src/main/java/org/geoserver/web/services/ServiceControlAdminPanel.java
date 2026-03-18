/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.ServiceInfo;

/**
 * Panel used to manage service enabled, and modes such as strict.
 *
 * <p>If a specific service type is provided, the ability to disable versions for that service will be presented inline.
 */
class ServiceControlAdminPanel<T extends ServiceInfo> extends AdminPagePanel {
    @Serial
    private static final long serialVersionUID = -1;

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(ServiceControlAdminPanel.class);

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

    public ServiceControlAdminPanel(String panelId, IModel<?> infoModel, String specificServiceType) {
        super(panelId, infoModel);

        // service control
        if (specificServiceType != null) {
            add(new Label(
                    "service.enabled",
                    new StringResourceModel("service.enabled", this).setParameters(specificServiceType)));
        } else {
            add(new Label("service.enabled", new StringResourceModel("service.enabled", this).setParameters("")));
        }
        CheckBox enabled = new CheckBox("enabled");
        enabled.setOutputMarkupId(true);
        enabled.setMarkupId("enabled");
        add(enabled);

        CheckBox citeCompliant = new CheckBox("citeCompliant");
        citeCompliant.setOutputMarkupId(true);
        citeCompliant.setMarkupId("citeCompliant");
        add(citeCompliant);

        if (specificServiceType != null) {
            add(new DisabledVersionsPanel(
                    "disabledVersions", new PropertyModel<>(infoModel, "disabledVersions"), specificServiceType));
        } else {
            Label placeholder = new Label("disabledVersions");
            placeholder.setVisible(false);
            add(placeholder);
        }
    }
}
