/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.IOException;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.image.io.ImageIOExt;
import org.geotools.util.logging.Logging;

public class GWCSettingsPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(GWCSettingsPage.class);

    public GWCSettingsPage() {
        setHeaderPanel(headerPanel());

        GWC gwc = GWC.get();
        // use a detached copy of gwc config to support the tabbed pane
        final GWCConfig gwcConfig = gwc.getConfig().clone();

        IModel<GWCConfig> formModel = new Model<>(gwcConfig);

        final Form<GWCConfig> form = new Form<>("form", formModel);
        add(form);

        final GWCServicesPanel gwcServicesPanel =
                new GWCServicesPanel("gwcServicesPanel", formModel);
        final CachingOptionsPanel defaultCachingOptionsPanel =
                new CachingOptionsPanel("cachingOptionsPanel", formModel);

        form.add(gwcServicesPanel);
        form.add(defaultCachingOptionsPanel);

        form.add(
                new Button("submit") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        save(form, true);
                    }
                });
        form.add(applyLink(form));
        form.add(
                new GeoServerAjaxFormLink("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form form) {
                        doReturn();
                    }
                });

        checkWarnings();
    }

    public void save(Form<GWCConfig> form, boolean doReturn) {
        GWC gwc = GWC.get();
        final IModel<GWCConfig> gwcConfigModel = form.getModel();
        GWCConfig gwcConfig = gwcConfigModel.getObject();
        try {
            gwc.saveConfig(gwcConfig);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error saving GWC config", e);
            form.error("Error saving GWC config: " + e.getMessage());
            return;
        }
        // Update ConfigurableBlobStore
        ConfigurableBlobStore blobstore = GeoServerExtensions.bean(ConfigurableBlobStore.class);
        if (blobstore != null) {
            blobstore.setChanged(gwcConfig, false);
        }
        // Do return
        if (doReturn) doReturn();
    }

    private GeoserverAjaxSubmitLink applyLink(Form form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.add(form);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target, Form<?> form) {
                try {
                    @SuppressWarnings("unchecked")
                    Form<GWCConfig> cast = (Form<GWCConfig>) form;
                    save(cast, false);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.add(form);
                }
            }
        };
    }

    private void checkWarnings() {
        Long imageIOFileCachingThreshold = ImageIOExt.getFilesystemThreshold();
        if (null == imageIOFileCachingThreshold || 0L >= imageIOFileCachingThreshold.longValue()) {
            String warningMsg =
                    new ResourceModel("GWC.ImageIOFileCachingThresholdUnsetWarning").getObject();
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
            AttributeModifier attributeModifier =
                    new AttributeModifier("title", new StringResourceModel(titleKey, null, null));
            checkBox.add(attributeModifier);
        }
        return checkBox;
    }
}
