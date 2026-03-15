package org.geoserver.web.services;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.config.ServiceInfo;

import java.io.Serial;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

/**
 * Panel used to manage service enabled, and modes such as strict.
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

    public ServiceControlAdminPanel(String panelId, IModel<T> infoModel, String specificServiceType) {
        super(panelId, infoModel);
        T service = infoModel.getObject();

        add(new DisabledVersionsPanel(
                "disabledVersions", new PropertyModel<>(infoModel, "disabledVersions"), specificServiceType));
    }
}
