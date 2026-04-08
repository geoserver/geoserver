/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.cors;

import java.io.IOException;
import java.io.Serial;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.web.admin.ServerAdminPage;

public class CORSConfigurationPage extends ServerAdminPage {
    private final IModel<GeoServerInfo> globalInfoModel;

    public CORSConfigurationPage() throws IOException {
        globalInfoModel = getGlobalInfoModel();
        Form<GeoServerInfo> form = new Form<>("form", new CompoundPropertyModel<>(globalInfoModel));
        add(form);
        PropertyModel<SettingsInfo> settingsModel = new PropertyModel<>(globalInfoModel, "settings");

        final CORSConfigurationPanel corsConfigurationPanel =
                new CORSConfigurationPanel("corsConfigurationPanel", settingsModel);
        form.add(corsConfigurationPanel);
        form.add(new SubmitLink("save", form) {
            @Serial
            private static final long serialVersionUID = -8900006356449444190L;

            @Override
            public void onSubmit() {
                saveConfiguration(true);
            }
        });
        form.add(new Button("apply") {
            @Serial
            private static final long serialVersionUID = -3327108044498697618L;

            @Override
            public void onSubmit() {
                saveConfiguration(false);
            }
        });
        form.add(new Button("cancel") {
            @Serial
            private static final long serialVersionUID = 7567566240344471893L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        });
        add(form);
    }

    /**
     * Saves the current configuration to the data directory.
     *
     * @param doReturn true to return to the home page
     */
    private void saveConfiguration(boolean doReturn) {
        try {
            GeoServer gs = getGeoServer();
            gs.save(globalInfoModel.getObject());

            if (doReturn) {
                doReturn();
            }
        } catch (Exception e) {
            error(e);
        }
    }
}
