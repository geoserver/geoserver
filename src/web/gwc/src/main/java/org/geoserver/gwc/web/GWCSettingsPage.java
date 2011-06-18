package org.geoserver.gwc.web;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.StorageUnit;

public class GWCSettingsPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(GWCSettingsPage.class);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GWCSettingsPage() {
        setHeaderPanel(headerPanel());

        final boolean diskQuotaDisabled = getGWC().getDisQuotaConfig() == null;
        final Form form = new Form("form");
        add(form);

        final IModel<WMSInfo> wmsInfoModel = new LoadableDetachableModel<WMSInfo>() {
            private static final long serialVersionUID = 1L;

            public WMSInfo load() {
                return getGeoServer().getService(WMSInfo.class);
            }
        };

        final IModel<DiskQuotaConfig> diskQuotaModel = new LoadableDetachableModel<DiskQuotaConfig>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected DiskQuotaConfig load() {
                final GWC gwc = getGWC();
                DiskQuotaConfig quotaConfig = gwc.getDisQuotaConfig();
                if (quotaConfig == null) {
                    quotaConfig = new DiskQuotaConfig();// fake
                    quotaConfig.setDefaults();
                }
                return quotaConfig;
            }
        };

        final IModel<GWC> gwcModel = new LoadableDetachableModel<GWC>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected GWC load() {
                return getGWC();
            }
        };

        PropertyModel<MetadataMap> metadataModel = new PropertyModel<MetadataMap>(wmsInfoModel,
                "metadata");
        IModel<Boolean> wmsIntegrationEnabledModel = new MapModel(metadataModel,
                GWC.WMS_INTEGRATION_ENABLED_KEY);

        CheckBox wmsIntegration = checkbox("enableWMSIntegration", wmsIntegrationEnabledModel,
                "GWCSettingsPage.enableWMSIntegration.title");
        form.add(wmsIntegration);

        final DiskQuotaConfig diskQuotaConfig = diskQuotaModel.getObject();
        Quota globalQuota = diskQuotaConfig.getGlobalQuota();
        if (globalQuota == null) {
            LOGGER.info("There's no GWC global disk quota configured, setting a default of 100MiB");
            globalQuota = new Quota(100, StorageUnit.MiB);
            diskQuotaConfig.setGlobalQuota(globalQuota);
        }

        // use this two payload models to let the user configure the global
        // quota as a decimal value
        // plus a storage unit. Then at form sumbission we'll transform them
        // back to a BigInteger
        // representing the quota byte count
        BigInteger bytes = globalQuota.getBytes();
        StorageUnit bestRepresentedUnit = StorageUnit.bestFit(bytes);
        BigDecimal transformedQuota = StorageUnit.B.convertTo(new BigDecimal(bytes),
                bestRepresentedUnit);
        final IModel<Double> configQuotaValueModel = new Model<Double>(
                transformedQuota.doubleValue());
        final IModel<StorageUnit> configQuotaUnitModel = new Model<StorageUnit>(bestRepresentedUnit);

        DiskQuotaConfigPanel diskQuotaConfigPanel = new DiskQuotaConfigPanel(
                "diskQuotaConfigPanel", form, diskQuotaModel, gwcModel, configQuotaValueModel,
                configQuotaUnitModel);
        if (diskQuotaDisabled) {
            diskQuotaConfigPanel.setVisible(false);
        }
        form.add(diskQuotaConfigPanel);

        form.add(new Button("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                GeoServer gs = getGeoServer();
                WMSInfo wmsInfo = wmsInfoModel.getObject();
                gs.save(wmsInfo);
                if (diskQuotaDisabled) {
                    setResponsePage(GeoServerHomePage.class);
                    return;
                }
                GWC gwc = getGWC();
                StorageUnit chosenUnit = configQuotaUnitModel.getObject();
                // REVISIT: it seems Wicket is sending back a plain string
                // instead of a BigDecimal
                String chosenQuotaStr = String.valueOf(configQuotaValueModel.getObject());
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
                gwc.getGlobalQuota().setValue(chosenQuota.doubleValue(), chosenUnit);
                gwc.saveDiskQuotaConfig();

                setResponsePage(GeoServerHomePage.class);
            }
        });
        form.add(new Button("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                setResponsePage(GeoServerHomePage.class);
            }
        });

    }

    private GWC getGWC() {
        final GWC gwc = (GWC) getGeoServerApplication().getBean("gwcFacade");
        return gwc;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        return header;
    }

    static CheckBox checkbox(String id, IModel<Boolean> model, String titleKey) {
        CheckBox checkBox = new CheckBox(id, model);
        if (null != titleKey) {
            checkBox.add(new AttributeModifier("title", true, new StringResourceModel(titleKey,
                    (Component) null, null)));
        }
        return checkBox;
    }
}
