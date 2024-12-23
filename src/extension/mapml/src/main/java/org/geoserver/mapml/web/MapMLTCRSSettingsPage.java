/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.web;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.web.admin.ServerAdminPage;

public class MapMLTCRSSettingsPage extends ServerAdminPage {

    private final IModel<GeoServerInfo> globalInfoModel;

    public MapMLTCRSSettingsPage() {

        globalInfoModel = getGlobalInfoModel();
        Form<GeoServerInfo> form = new Form<>("form", new CompoundPropertyModel<>(globalInfoModel));
        add(form);

        PropertyModel<SettingsInfo> settingsModel = new PropertyModel<>(globalInfoModel, "settings");

        MapMLTCRSSettingsPanel panel = new MapMLTCRSSettingsPanel("mapMLTCRS", settingsModel);
        form.add(panel);

        Button submit = new Button("submit") {

            @Override
            public void onSubmit() {
                save(true);
            }
        };
        form.add(submit);

        Button cancel = new Button("cancel") {

            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    private void save(boolean doReturn) {
        GeoServer gs = getGeoServer();
        gs.save(globalInfoModel.getObject());
        TiledCRSConstants.reloadDefinitions();
        if (doReturn) doReturn();
    }
}
