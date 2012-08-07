/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.image.io.ImageIOExt;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.storage.StorageUnit;

public class DiskQuotaSettingsPage extends GeoServerSecuredPage {

    public DiskQuotaSettingsPage() {
        GWC gwc = getGWC();

        final boolean diskQuotaModuleDisabled = gwc.getDiskQuotaConfig() == null;

        // use a dettached copy of dq config to support the tabbed pane
        final DiskQuotaConfig diskQuotaConfig;
        if (diskQuotaModuleDisabled) {
            diskQuotaConfig = new DiskQuotaConfig();// fake
            diskQuotaConfig.setDefaults();
        } else {
            diskQuotaConfig = gwc.getDiskQuotaConfig().clone();
        }

        final Form<Map<String, Serializable>> form;
        form = new Form<Map<String, Serializable>>("form");
        add(form);

        final IModel<DiskQuotaConfig> diskQuotaModel = new Model<DiskQuotaConfig>(diskQuotaConfig);

        final DiskQuotaConfigPanel diskQuotaConfigPanel = new DiskQuotaConfigPanel(
                "diskQuotaPanel", diskQuotaModel);
        
        if (diskQuotaModuleDisabled) {
            diskQuotaConfigPanel.setEnabled(false);
            super.warn(new ResourceModel("DiskQuotaSettingsPage.disabledWarning").getObject());
        }
        
        form.add(diskQuotaConfigPanel);

        form.add(new Button("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                GWC gwc = getGWC();
                if (!diskQuotaModuleDisabled) {
                    StorageUnit chosenUnit = diskQuotaConfigPanel.getStorageUnit();
                    // REVISIT: it seems Wicket is sending back a plain string instead of a
                    String chosenQuotaStr = String.valueOf(diskQuotaConfigPanel.getQuotaValue());
                    Double chosenQuota;
                    try {
                        chosenQuota = Double.valueOf(chosenQuotaStr);
                    } catch (NumberFormatException e) {
                        form.error(chosenQuotaStr + " is not a valid floating point number");// TODO:
                        // localize
                        return;
                    }
                    if (chosenQuota.doubleValue() <= 0D) {
                        form.error("Quota has to be > 0");
                        return;
                    }
                    DiskQuotaConfig dqConfig = diskQuotaModel.getObject();
                    dqConfig.getGlobalQuota().setValue(chosenQuota.doubleValue(), chosenUnit);
                    gwc.saveDiskQuotaConfig(dqConfig);
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

    private GWC getGWC() {
        final GWC gwc = (GWC) getGeoServerApplication().getBean("gwcFacade");
        return gwc;
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
