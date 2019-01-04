/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.ExpirationPolicy;
import org.geowebcache.diskquota.QuotaStoreFactory;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStoreFactory;
import org.geowebcache.diskquota.jdbc.SQLDialect;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.StorageUnit;
import org.springframework.context.ApplicationContext;

/**
 * A panel to configure the global disk quota settings.
 *
 * @author groldan
 */
public class DiskQuotaConfigPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logging.getLogger(DiskQuotaConfigPanel.class);

    private final IModel<StorageUnit> configQuotaUnitModel;

    private final IModel<Double> configQuotaValueModel;

    public DiskQuotaConfigPanel(
            final String id,
            final IModel<DiskQuotaConfig> diskQuotaConfigModel,
            final IModel<JDBCConfiguration> jdbcQuotaConfigModel) {
        super(id, diskQuotaConfigModel);

        final DiskQuotaConfig diskQuotaConfig = diskQuotaConfigModel.getObject();

        Quota globalQuota = diskQuotaConfig.getGlobalQuota();
        if (globalQuota == null) {
            LOGGER.info("There's no GWC global disk quota configured, setting a default of 100MiB");
            globalQuota = new Quota(100, StorageUnit.MiB);
            diskQuotaConfig.setGlobalQuota(globalQuota);
        }

        // use this two payload models to let the user configure the global quota as a decimal value
        // plus a storage unit. Then at form sumbission we'll transform them back to a BigInteger
        // representing the quota byte count
        BigInteger bytes = globalQuota.getBytes();
        StorageUnit bestRepresentedUnit = StorageUnit.bestFit(bytes);
        BigDecimal transformedQuota =
                StorageUnit.B.convertTo(new BigDecimal(bytes), bestRepresentedUnit);
        configQuotaValueModel = new Model<Double>(transformedQuota.doubleValue());

        configQuotaUnitModel = new Model<StorageUnit>(bestRepresentedUnit);

        addDiskQuotaIntegrationEnablement(diskQuotaConfigModel);

        addDiskQuotaStoreChooser(diskQuotaConfigModel, jdbcQuotaConfigModel);

        addCleanUpFrequencyConfig(diskQuotaConfigModel);

        addGlobalQuotaConfig(diskQuotaConfigModel, configQuotaValueModel, configQuotaUnitModel);

        addGlobalExpirationPolicyConfig(diskQuotaConfigModel);
    }

    private void addDiskQuotaStoreChooser(
            IModel<DiskQuotaConfig> diskQuotaModel,
            IModel<JDBCConfiguration> jdbcQuotaConfigModel) {
        final WebMarkupContainer quotaStoreContainer =
                new WebMarkupContainer("quotaStoreContainer");
        quotaStoreContainer.setOutputMarkupId(true);
        add(quotaStoreContainer);

        // get the list of supported quota store types
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        Map<String, QuotaStoreFactory> factories =
                applicationContext.getBeansOfType(QuotaStoreFactory.class);
        List<String> storeNames = new ArrayList<String>();
        for (QuotaStoreFactory sf : factories.values()) {
            storeNames.addAll(sf.getSupportedStoreNames());
        }
        Collections.sort(storeNames);

        // add the drop down chooser
        PropertyModel<String> storeNameModel =
                new PropertyModel<String>(diskQuotaModel, "quotaStore");
        if (diskQuotaModel.getObject().getQuotaStore() == null) {
            storeNameModel.setObject(JDBCQuotaStoreFactory.H2_STORE);
        }
        final DropDownChoice<String> quotaStoreChooser =
                new DropDownChoice<String>(
                        "diskQuotaStore",
                        storeNameModel,
                        storeNames,
                        new LocalizedChoiceRenderer(this));
        quotaStoreChooser.setOutputMarkupId(true);
        quotaStoreContainer.add(quotaStoreChooser);

        // add the JDBC container
        final WebMarkupContainer jdbcContainer = new WebMarkupContainer("jdbcQuotaStoreContainer");
        jdbcContainer.setOutputMarkupId(true);
        jdbcContainer.setVisible("JDBC".equals(quotaStoreChooser.getModelObject()));
        quotaStoreContainer.add(jdbcContainer);

        // add a chooser for the dialect type
        List<String> dialectBeanNames =
                new ArrayList<String>(applicationContext.getBeansOfType(SQLDialect.class).keySet());
        List<String> dialectNames = new ArrayList<String>();
        for (String beanName : dialectBeanNames) {
            int idx = beanName.indexOf("QuotaDialect");
            if (idx > 0) {
                dialectNames.add(beanName.substring(0, idx));
            }
        }
        JDBCConfiguration config = jdbcQuotaConfigModel.getObject();
        IModel<String> dialectModel = new PropertyModel<String>(jdbcQuotaConfigModel, "dialect");
        DropDownChoice<String> dialectChooser =
                new DropDownChoice<String>("dialectChooser", dialectModel, dialectNames);
        dialectChooser.setRequired(true);
        jdbcContainer.add(dialectChooser);

        // add a chooser for the connection type
        List<String> connectionTypes = Arrays.asList("JNDI", "PRIVATE_POOL");
        Model<String> connectionTypeModel = new Model<String>();
        if (config.getJNDISource() == null) {
            connectionTypeModel.setObject("PRIVATE_POOL");
        } else {
            connectionTypeModel.setObject("JNDI");
        }
        final DropDownChoice<String> connectionTypeChooser =
                new DropDownChoice<String>(
                        "connectionTypeChooser",
                        connectionTypeModel,
                        connectionTypes,
                        new LocalizedChoiceRenderer(this));
        connectionTypeChooser.setOutputMarkupId(true);
        jdbcContainer.add(connectionTypeChooser);

        // make the JDBC configuration visible only when the user chose a JDBC store
        quotaStoreChooser.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = -6806581935751265393L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        jdbcContainer.setVisible("JDBC".equals(quotaStoreChooser.getModelObject()));
                        target.add(quotaStoreContainer);
                    }
                });

        // a container for jndi and local private pool options
        final WebMarkupContainer connectionTypeContainer =
                new WebMarkupContainer("connectionTypeContainer");
        connectionTypeContainer.setOutputMarkupId(true);
        jdbcContainer.add(connectionTypeContainer);

        // add a field for editing the JNDI connection parameters
        final WebMarkupContainer jndiContainer = new WebMarkupContainer("jndiLocationContainer");
        jndiContainer.setVisible(config.getJNDISource() != null);
        connectionTypeContainer.add(jndiContainer);
        IModel<String> jndiModel = new PropertyModel<String>(jdbcQuotaConfigModel, "jNDISource");
        TextField<String> jndiLocation = new TextField<String>("jndiLocation", jndiModel);
        jndiLocation.setRequired(true);
        jndiContainer.add(jndiLocation);

        // and a panel to edit the private jdbc pool
        IModel<JDBCConfiguration.ConnectionPoolConfiguration> poolConfigurationModel =
                new PropertyModel<JDBCConfiguration.ConnectionPoolConfiguration>(
                        jdbcQuotaConfigModel, "connectionPool");
        final JDBCConnectionPoolPanel privatePoolPanel =
                new JDBCConnectionPoolPanel("connectionPoolConfigurator", poolConfigurationModel);
        privatePoolPanel.setVisible(config.getJNDISource() == null);
        connectionTypeContainer.add(privatePoolPanel);

        // make the two ways to configure the JDBC store show up as alternatives
        connectionTypeChooser.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = -8286073946292214144L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        boolean jndiVisible = "JNDI".equals(connectionTypeChooser.getModelObject());
                        jndiContainer.setVisible(jndiVisible);
                        privatePoolPanel.setVisible(!jndiVisible);
                        target.add(connectionTypeContainer);
                    }
                });
    }

    private void addGlobalQuotaConfig(
            final IModel<DiskQuotaConfig> diskQuotaModel,
            IModel<Double> quotaValueModel,
            IModel<StorageUnit> unitModel) {

        final IModel<Quota> globalQuotaModel =
                new PropertyModel<Quota>(diskQuotaModel, "globalQuota");

        final IModel<Quota> globalUsedQuotaModel =
                new LoadableDetachableModel<Quota>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Quota load() {
                        GWC gwc = GWC.get();
                        if (!gwc.isDiskQuotaAvailable()) {
                            return new Quota(); // fake
                        }
                        return gwc.getGlobalUsedQuota();
                    }
                };

        Object[] progressMessageParams = {
            globalUsedQuotaModel.getObject().toNiceString(),
            globalQuotaModel.getObject().toNiceString()
        };
        IModel<String> progressMessageModel =
                new StringResourceModel("DiskQuotaConfigPanel.usedQuotaMessage")
                        .setParameters(progressMessageParams);
        addGlobalQuotaStatusBar(globalQuotaModel, globalUsedQuotaModel, progressMessageModel);

        TextField<Double> quotaValue = new TextField<Double>("globalQuota", quotaValueModel);
        quotaValue.setRequired(true);
        add(quotaValue);

        List<? extends StorageUnit> units =
                Arrays.asList(StorageUnit.MiB, StorageUnit.GiB, StorageUnit.TiB);
        DropDownChoice<StorageUnit> quotaUnitChoice;
        quotaUnitChoice = new DropDownChoice<StorageUnit>("globalQuotaUnits", unitModel, units);
        add(quotaUnitChoice);
    }

    private void addGlobalExpirationPolicyConfig(final IModel<DiskQuotaConfig> diskQuotaModel) {
        IModel<ExpirationPolicy> globalQuotaPolicyModel =
                new PropertyModel<ExpirationPolicy>(diskQuotaModel, "globalExpirationPolicyName");

        RadioGroup<ExpirationPolicy> globalQuotaPolicy;
        globalQuotaPolicy =
                new RadioGroup<ExpirationPolicy>(
                        "globalQuotaExpirationPolicy", globalQuotaPolicyModel);
        add(globalQuotaPolicy);

        IModel<ExpirationPolicy> lfuModel = new Model<ExpirationPolicy>(ExpirationPolicy.LFU);
        IModel<ExpirationPolicy> lruModel = new Model<ExpirationPolicy>(ExpirationPolicy.LRU);

        Radio<ExpirationPolicy> globalQuotaPolicyLFU;
        Radio<ExpirationPolicy> globalQuotaPolicyLRU;
        globalQuotaPolicyLFU = new Radio<ExpirationPolicy>("globalQuotaPolicyLFU", lfuModel);
        globalQuotaPolicyLRU = new Radio<ExpirationPolicy>("globalQuotaPolicyLRU", lruModel);

        globalQuotaPolicy.add(globalQuotaPolicyLFU);
        globalQuotaPolicy.add(globalQuotaPolicyLRU);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addCleanUpFrequencyConfig(final IModel<DiskQuotaConfig> diskQuotaModel) {

        final DiskQuotaConfig diskQuotaConfig = diskQuotaModel.getObject();

        int frequency = diskQuotaConfig.getCacheCleanUpFrequency();
        TimeUnit unit = diskQuotaConfig.getCacheCleanUpUnits();
        if (TimeUnit.SECONDS != unit) {
            frequency = (int) TimeUnit.SECONDS.convert(frequency, unit);
            diskQuotaConfig.setCacheCleanUpFrequency(frequency);
            diskQuotaConfig.setCacheCleanUpUnits(TimeUnit.SECONDS);
        }

        IModel<Integer> cleanUpFreqModel;
        cleanUpFreqModel = new PropertyModel<Integer>(diskQuotaModel, "cacheCleanUpFrequency");
        TextField<Integer> cleanUpFreq = new TextField<Integer>("cleanUpFreq", cleanUpFreqModel);
        cleanUpFreq.setRequired(true);
        cleanUpFreq.add(
                new AttributeModifier(
                        "title",
                        new StringResourceModel(
                                "DiskQuotaConfigPanel.cleanUpFreq.title", (Component) null, null)));
        add(cleanUpFreq);
        {
            Date lastRun = diskQuotaConfig.getLastCleanUpTime();
            String resourceId;
            HashMap<String, String> params = new HashMap<String, String>();
            if (lastRun == null) {
                resourceId = "DiskQuotaConfigPanel.cleanUpLastRunNever";
            } else {
                resourceId = "DiskQuotaConfigPanel.cleanUpLastRun";
                long timeAgo = (System.currentTimeMillis() - lastRun.getTime()) / 1000;
                String timeUnits = "s";
                if (timeAgo > 60 * 60 * 24) {
                    timeUnits = "d";
                    timeAgo /= 60 * 60 * 24;
                } else if (timeAgo > 60 * 60) {
                    timeUnits = "h";
                    timeAgo /= 60 * 60;
                } else if (timeAgo > 60) {
                    timeUnits = "m";
                    timeAgo /= 60;
                }
                params.put("x", String.valueOf(timeAgo));
                params.put("timeUnit", timeUnits);
            }
            IModel<String> lastRunModel =
                    new StringResourceModel(resourceId, this, new Model(params));
            add(new Label("cleanUpLastRun", lastRunModel));
        }
    }

    private void addDiskQuotaIntegrationEnablement(IModel<DiskQuotaConfig> diskQuotaModel) {
        IModel<Boolean> quotaEnablementModel =
                new PropertyModel<Boolean>(diskQuotaModel, "enabled");
        CheckBox diskQuotaIntegration =
                checkbox(
                        "enableDiskQuota",
                        quotaEnablementModel,
                        "DiskQuotaConfigPanel.enableDiskQuota.title");
        add(diskQuotaIntegration);
    }

    private void addGlobalQuotaStatusBar(
            final IModel<Quota> globalQuotaModel,
            final IModel<Quota> globalUsedQuotaModel,
            IModel<String> progressMessageModel) {

        Quota limit = globalQuotaModel.getObject();
        Quota used = globalUsedQuotaModel.getObject();

        BigInteger limitValue = limit.getBytes();
        BigInteger usedValue = used.getBytes();

        StorageUnit bestUnitForLimit = StorageUnit.bestFit(limitValue);
        StorageUnit bestUnitForUsed = StorageUnit.bestFit(usedValue);

        StorageUnit biggerUnit =
                bestUnitForLimit.compareTo(bestUnitForUsed) > 0
                        ? bestUnitForLimit
                        : bestUnitForUsed;

        BigDecimal showLimit = StorageUnit.B.convertTo(new BigDecimal(limitValue), biggerUnit);
        BigDecimal showUsed = StorageUnit.B.convertTo(new BigDecimal(usedValue), biggerUnit);

        final IModel<Number> limitModel = new Model<Number>(showLimit);
        final IModel<Number> usedModel = new Model<Number>(showUsed);

        StatusBar statusBar =
                new StatusBar(
                        "globalQuotaProgressBar", limitModel, usedModel, progressMessageModel);

        add(statusBar);
    }

    public StorageUnit getStorageUnit() {
        return configQuotaUnitModel.getObject();
    }

    public Object getQuotaValue() {
        // REVISIT: it seems Wicket is sending back a plain string instead of a BigDecimal
        return configQuotaValueModel.getObject();
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
