/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.image.io.ImageIOExt;
import org.geotools.util.logging.Logging;

public class GWCSettingsPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(GWCSettingsPage.class);

    private IModel<Map<String, Serializable>> formModel;

    public GWCSettingsPage() {
        setHeaderPanel(headerPanel());

        GWC gwc = GWC.get();
        // use a dettached copy of gwc config to support the tabbed pane
        final GWCConfig gwcConfig = gwc.getConfig().clone();

        Map<String, Serializable> formData = new HashMap<String, Serializable>();
        formData.put("gwcConfig", gwcConfig);
        formModel = new MapModel<String, Serializable>(formData);

        final Form<Map<String, Serializable>> form;
        form = new Form<Map<String, Serializable>>("form", formModel);
        add(form);

        final IModel<GWCConfig> gwcConfigModel = new PropertyModel<GWCConfig>(formModel,
                "gwcConfig");

        final GWCServicesPanel gwcServicesPanel = new GWCServicesPanel("gwcServicesPanel",
                gwcConfigModel);
        final CachingOptionsPanel defaultCachingOptionsPanel = new CachingOptionsPanel(
                "cachingOptionsPanel", gwcConfigModel);

        form.add(gwcServicesPanel);
        form.add(defaultCachingOptionsPanel);

        form.add(new Button("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                GWC gwc = GWC.get();
                final IModel formModel = form.getModel();
                final IModel<GWCConfig> gwcConfigModel = new PropertyModel<GWCConfig>(formModel,
                        "gwcConfig");
                GWCConfig gwcConfig = gwcConfigModel.getObject();
                try {
                    gwc.saveConfig(gwcConfig);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error saving GWC config", e);
                    form.error("Error saving GWC config: " + e.getMessage());
                    return;
                }

                doReturn();
            }
        });
        form.add(new GeoServerAjaxFormLink("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                doReturn();
            }

        });

        checkWarnings();
    }

    private void checkWarnings() {
        Long imageIOFileCachingThreshold = ImageIOExt.getFilesystemThreshold();
        if (null == imageIOFileCachingThreshold || 0L >= imageIOFileCachingThreshold.longValue()) {
            String warningMsg = new ResourceModel("GWC.ImageIOFileCachingThresholdUnsetWarning")
                    .getObject();
            super.warn(warningMsg);
        }
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        return header;
    }

    static CheckBox checkbox(String id, IModel<Boolean> model, String titleKey) {
        CheckBox checkBox = new CheckBox(id, model);
        if (null != titleKey) {
            AttributeModifier attributeModifier = new AttributeModifier("title", true,
                    new StringResourceModel(titleKey, (Component) null, null));
            checkBox.add(attributeModifier);
        }
        return checkBox;
    }
}
