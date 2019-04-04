/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
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
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.image.io.ImageIOExt;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.storage.StorageUnit;

public class DiskQuotaSettingsPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 75816375328629448L;

    public DiskQuotaSettingsPage() throws Exception {
        GWC gwc = getGWC();

        final boolean diskQuotaModuleDisabled = gwc.getDiskQuotaConfig() == null;

        // get the quota store config, show an error message in case the quota
        // store loading failed
        ConfigurableQuotaStoreProvider provider =
                GeoServerApplication.get().getBeanOfType(ConfigurableQuotaStoreProvider.class);
        if (provider.getException() != null) {
            ParamResourceModel rm =
                    new ParamResourceModel(
                            "GWC.diskQuotaLoadFailed", null, provider.getException().getMessage());
            error(rm.getString());
        }

        // use a dettached copy of dq config to support the tabbed pane
        final DiskQuotaConfig diskQuotaConfig;
        if (diskQuotaModuleDisabled) {
            diskQuotaConfig = new DiskQuotaConfig(); // fake
            diskQuotaConfig.setDefaults();
        } else {
            diskQuotaConfig = gwc.getDiskQuotaConfig().clone();
        }

        // same as above, but we don't need to create a copy of the JDBC quota config since
        // that config is just used to instantiate the quota store, and then gets promptly discarted
        final JDBCConfiguration jdbcQuotaConfiguration;
        if (gwc.getJDBCDiskQuotaConfig() == null) {
            jdbcQuotaConfiguration = new JDBCConfiguration();
            JDBCConfiguration.ConnectionPoolConfiguration configuration =
                    new JDBCConfiguration.ConnectionPoolConfiguration();
            configuration.setMinConnections(1);
            configuration.setMaxConnections(10);
            configuration.setConnectionTimeout(10000);
            configuration.setMaxOpenPreparedStatements(50);
            jdbcQuotaConfiguration.setConnectionPool(configuration);
        } else {
            jdbcQuotaConfiguration = gwc.getJDBCDiskQuotaConfig();
        }

        final Form<Map<String, Serializable>> form;
        form = new Form<Map<String, Serializable>>("form");
        add(form);

        final IModel<DiskQuotaConfig> diskQuotaModel = new Model<DiskQuotaConfig>(diskQuotaConfig);
        final IModel<JDBCConfiguration> jdbcQuotaModel =
                new Model<JDBCConfiguration>(jdbcQuotaConfiguration);

        final DiskQuotaConfigPanel diskQuotaConfigPanel =
                new DiskQuotaConfigPanel("diskQuotaPanel", diskQuotaModel, jdbcQuotaModel);

        if (diskQuotaModuleDisabled) {
            diskQuotaConfigPanel.setEnabled(false);
            super.warn(new ResourceModel("DiskQuotaSettingsPage.disabledWarning").getObject());
        }

        form.add(diskQuotaConfigPanel);

        form.add(
                new Button("submit") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        GWC gwc = getGWC();
                        if (!diskQuotaModuleDisabled) {
                            StorageUnit chosenUnit = diskQuotaConfigPanel.getStorageUnit();
                            // REVISIT: it seems Wicket is sending back a plain string instead of a
                            String chosenQuotaStr =
                                    String.valueOf(diskQuotaConfigPanel.getQuotaValue());
                            Double chosenQuota;
                            try {
                                chosenQuota = Double.valueOf(chosenQuotaStr);
                            } catch (NumberFormatException e) {
                                form.error(
                                        chosenQuotaStr
                                                + " is not a valid floating point number"); // TODO:
                                // localize
                                return;
                            }
                            if (chosenQuota.doubleValue() <= 0D) {
                                form.error("Quota has to be > 0");
                                return;
                            }
                            DiskQuotaConfig dqConfig = diskQuotaModel.getObject();
                            JDBCConfiguration jdbcConfig = jdbcQuotaModel.getObject();
                            if (dqConfig.getQuotaStore() != null
                                    && dqConfig.getQuotaStore().equals("JDBC")) {
                                try {
                                    gwc.testQuotaConfiguration(jdbcConfig);
                                } catch (Exception e) {
                                    LOGGER.log(
                                            Level.SEVERE,
                                            "Error instantiating the JDBC configuration",
                                            e);
                                    error(
                                            "Failure occurred while saving the JDBC configuration"
                                                    + e.getMessage()
                                                    + " (see the logs for a full stack trace)");
                                    return;
                                }
                            }

                            dqConfig.getGlobalQuota()
                                    .setValue(chosenQuota.doubleValue(), chosenUnit);
                            try {
                                gwc.saveDiskQuotaConfig(dqConfig, jdbcConfig.clone(false));
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.SEVERE, "Failed to save the JDBC configuration", e);
                                error(
                                        "Failure occurred while saving the JDBC configuration"
                                                + e.getMessage()
                                                + " (see the logs for a full stack trace)");
                                return;
                            }
                        }

                        doReturn();
                    }
                });
        form.add(
                new GeoServerAjaxFormLink("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        doReturn();
                    }
                });

        checkWarnings();
    }

    private void checkWarnings() {
        Long imageIOFileCachingThreshold = ImageIOExt.getFilesystemThreshold();
        if (null == imageIOFileCachingThreshold || 0L >= imageIOFileCachingThreshold.longValue()) {
            String warningMsg =
                    new ResourceModel("GWC.ImageIOFileCachingThresholdUnsetWarning").getObject();
            super.warn(warningMsg);
        }
    }

    private GWC getGWC() {
        final GWC gwc = (GWC) getGeoServerApplication().getBean("gwcFacade");
        gwc.syncEnv();
        return gwc;
    }

    static CheckBox checkbox(String id, IModel<Boolean> model, String titleKey) {
        CheckBox checkBox = new CheckBox(id, model);
        if (null != titleKey) {
            AttributeModifier attributeModifier =
                    new AttributeModifier(
                            "title", new StringResourceModel(titleKey, (Component) null, null));
            checkBox.add(attributeModifier);
        }
        return checkBox;
    }
}
