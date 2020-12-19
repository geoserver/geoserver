/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static org.geoserver.gwc.web.GWCSettingsPage.checkbox;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.config.GWCConfig;

public class GWCServicesPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public GWCServicesPanel(final String id, final IModel<GWCConfig> gwcConfigModel) {
        super(id, gwcConfigModel);

        final IModel<Boolean> wmsIntegrationEnabledModel =
                new PropertyModel<>(gwcConfigModel, "directWMSIntegrationEnabled");
        final IModel<Boolean> requiredTiledParamEnabledModel =
                new PropertyModel<>(gwcConfigModel, "requireTiledParameter");
        final IModel<Boolean> wmsCEnabledModel = new PropertyModel<>(gwcConfigModel, "WMSCEnabled");
        final IModel<Boolean> tmsEnabledModel = new PropertyModel<>(gwcConfigModel, "TMSEnabled");
        final IModel<Boolean> securityEnabledModel =
                new PropertyModel<>(gwcConfigModel, "securityEnabled");

        add(
                checkbox(
                        "enableWMSIntegration",
                        wmsIntegrationEnabledModel,
                        "GWCSettingsPage.enableWMSIntegration.title"));
        add(
                checkbox(
                        "requireTiledParameter",
                        requiredTiledParamEnabledModel,
                        "GWCSettingsPage.requireTiledParameter.title"));
        add(checkbox("enableWMSC", wmsCEnabledModel, "GWCSettingsPage.enableWMSC.title"));
        add(checkbox("enableTMS", tmsEnabledModel, "GWCSettingsPage.enableTMS.title"));
        add(
                checkbox(
                        "enableSecurity",
                        securityEnabledModel,
                        "GWCSettingsPage.enableSecurity.title"));
    }
}
