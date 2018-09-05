/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CachedRuleReader;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.config.GeoFenceConfigurationController;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.model.ExtPropertyModel;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

/**
 * GeoFence wicket administration UI for GeoServer.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class GeofencePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 5845823599005718408L;

    /** Configuration object. */
    private GeoFenceConfiguration config;

    private CacheConfiguration cacheParams;

    public GeofencePage() {
        // extracts cfg object from the registered probe instance
        GeoFenceConfigurationManager configManager =
                GeoServerExtensions.bean(GeoFenceConfigurationManager.class);

        config = configManager.getConfiguration().clone();
        cacheParams = configManager.getCacheConfiguration().clone();

        final IModel<GeoFenceConfiguration> configModel = getGeoFenceConfigModel();
        final IModel<CacheConfiguration> cacheModel = getCacheConfigModel();
        Form<IModel<GeoFenceConfiguration>> form =
                new Form<IModel<GeoFenceConfiguration>>(
                        "form",
                        new CompoundPropertyModel<IModel<GeoFenceConfiguration>>(configModel));
        form.setOutputMarkupId(true);
        add(form);
        form.add(
                new TextField<String>(
                                "instanceName",
                                new PropertyModel<String>(configModel, "instanceName"))
                        .setRequired(true));
        // .setVisible(!config.isInternal());
        form.add(
                new TextField<String>(
                                "servicesUrl",
                                new ExtPropertyModel<String>(configModel, "servicesUrl")
                                        .setReadOnly(config.isInternal()))
                        .setRequired(true)
                        .setEnabled(!config.isInternal()));

        form.add(
                new AjaxSubmitLink("test") {
                    private static final long serialVersionUID = -91239899377941223L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ((FormComponent<?>) form.get("servicesUrl")).processInput();
                        String servicesUrl =
                                (String)
                                        ((FormComponent<?>) form.get("servicesUrl"))
                                                .getConvertedInput();
                        RuleReaderService ruleReader = getRuleReaderService(servicesUrl);
                        try {
                            ruleReader.getMatchingRules(new RuleFilter());

                            info(
                                    new StringResourceModel(
                                                    GeofencePage.class.getSimpleName()
                                                            + ".connectionSuccessful")
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, e.getMessage(), e);
                        }

                        if (getPage() instanceof GeoServerBasePage) {
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        }
                    }

                    private RuleReaderService getRuleReaderService(String servicesUrl) {
                        if (config.isInternal()) {
                            return (RuleReaderService)
                                    GeoServerExtensions.bean("ruleReaderService");
                        } else {
                            HttpInvokerProxyFactoryBean invoker =
                                    new org.springframework.remoting.httpinvoker
                                            .HttpInvokerProxyFactoryBean();
                            invoker.setServiceUrl(servicesUrl);
                            invoker.setServiceInterface(RuleReaderService.class);
                            invoker.afterPropertiesSet();
                            return (RuleReaderService) invoker.getObject();
                        }
                    }
                }.setDefaultFormProcessing(false));

        form.add(
                new CheckBox(
                        "allowRemoteAndInlineLayers",
                        new PropertyModel<Boolean>(configModel, "allowRemoteAndInlineLayers")));
        form.add(
                new CheckBox(
                        "grantWriteToWorkspacesToAuthenticatedUsers",
                        new PropertyModel<Boolean>(
                                configModel, "grantWriteToWorkspacesToAuthenticatedUsers")));
        form.add(
                new CheckBox(
                        "useRolesToFilter",
                        new PropertyModel<Boolean>(configModel, "useRolesToFilter")));

        form.add(
                new TextField<String>(
                        "acceptedRoles", new PropertyModel<String>(configModel, "acceptedRoles")));

        Button submit =
                new Button("submit") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        try {
                            // save the changed configuration
                            GeoServerExtensions.bean(GeoFenceConfigurationController.class)
                                    .storeConfiguration(config, cacheParams);
                            doReturn();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Save error", e);
                            error(e);
                        }
                    }
                };
        form.add(submit);

        Button cancel =
                new Button("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        doReturn();
                    }
                }.setDefaultFormProcessing(false);
        form.add(cancel);

        form.add(
                new TextField<Long>("cacheSize", new PropertyModel<Long>(cacheModel, "size"))
                        .setRequired(true));

        form.add(
                new TextField<Long>(
                                "cacheRefresh",
                                new PropertyModel<Long>(cacheModel, "refreshMilliSec"))
                        .setRequired(true));

        form.add(
                new TextField<Long>(
                                "cacheExpire",
                                new PropertyModel<Long>(cacheModel, "expireMilliSec"))
                        .setRequired(true));

        CachedRuleReader cacheRuleReader = GeoServerExtensions.bean(CachedRuleReader.class);

        updateStatsValues(cacheRuleReader);

        for (String key : statsValues.keySet()) {
            Label label = new Label(key, new MapModel<String>(statsValues, key));
            label.setOutputMarkupId(true);
            form.add(label);
            statsLabels.add(label);
        }

        form.add(
                new AjaxSubmitLink("invalidate") {

                    private static final long serialVersionUID = 3847903240475052867L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        CachedRuleReader cacheRuleReader =
                                GeoServerExtensions.bean(CachedRuleReader.class);
                        cacheRuleReader.invalidateAll();
                        info(
                                new StringResourceModel(
                                                GeofencePage.class.getSimpleName()
                                                        + ".cacheInvalidated")
                                        .getObject());
                        updateStatsValues(cacheRuleReader);
                        for (Label label : statsLabels) {
                            target.add(label);
                        }

                        if (getPage() instanceof GeoServerBasePage) {
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        }
                    }
                }.setDefaultFormProcessing(false));
    }

    private final Map<String, Object> statsValues = new HashMap<String, Object>();

    private final Set<Label> statsLabels = new HashSet<Label>();

    private static final String KEY_RULE_SIZE = "rule.size";

    private static final String KEY_RULE_HIT = "rule.hit";

    private static final String KEY_RULE_MISS = "rule.miss";

    private static final String KEY_RULE_LOADOK = "rule.loadok";

    private static final String KEY_RULE_LOADKO = "rule.loadko";

    private static final String KEY_RULE_LOADTIME = "rule.loadtime";

    private static final String KEY_RULE_EVICTION = "rule.evict";

    private static final String KEY_ADMIN_SIZE = "admin.size";

    private static final String KEY_ADMIN_HIT = "admin.hit";

    private static final String KEY_ADMIN_MISS = "admin.miss";

    private static final String KEY_ADMIN_LOADOK = "admin.loadok";

    private static final String KEY_ADMIN_LOADKO = "admin.loadko";

    private static final String KEY_ADMIN_LOADTIME = "admin.loadtime";

    private static final String KEY_ADMIN_EVICTION = "admin.evict";

    private static final String KEY_USER_SIZE = "user.size";

    private static final String KEY_USER_HIT = "user.hit";

    private static final String KEY_USER_MISS = "user.miss";

    private static final String KEY_USER_LOADOK = "user.loadok";

    private static final String KEY_USER_LOADKO = "user.loadko";

    private static final String KEY_USER_LOADTIME = "user.loadtime";

    private static final String KEY_USER_EVICTION = "user.evict";

    private void updateStatsValues(CachedRuleReader cacheRuleReader) {

        statsValues.put(KEY_RULE_SIZE, "" + cacheRuleReader.getCacheSize());
        statsValues.put(KEY_RULE_HIT, "" + cacheRuleReader.getStats().hitCount());
        statsValues.put(KEY_RULE_MISS, "" + cacheRuleReader.getStats().missCount());
        statsValues.put(KEY_RULE_LOADOK, "" + cacheRuleReader.getStats().loadSuccessCount());
        statsValues.put(KEY_RULE_LOADKO, "" + cacheRuleReader.getStats().loadExceptionCount());
        statsValues.put(KEY_RULE_LOADTIME, "" + cacheRuleReader.getStats().totalLoadTime());
        statsValues.put(KEY_RULE_EVICTION, "" + cacheRuleReader.getStats().evictionCount());

        statsValues.put(KEY_ADMIN_SIZE, "" + cacheRuleReader.getAdminAuthCacheSize());
        statsValues.put(KEY_ADMIN_HIT, "" + cacheRuleReader.getAdminAuthStats().hitCount());
        statsValues.put(KEY_ADMIN_MISS, "" + cacheRuleReader.getAdminAuthStats().missCount());
        statsValues.put(
                KEY_ADMIN_LOADOK, "" + cacheRuleReader.getAdminAuthStats().loadSuccessCount());
        statsValues.put(
                KEY_ADMIN_LOADKO, "" + cacheRuleReader.getAdminAuthStats().loadExceptionCount());
        statsValues.put(
                KEY_ADMIN_LOADTIME, "" + cacheRuleReader.getAdminAuthStats().totalLoadTime());
        statsValues.put(
                KEY_ADMIN_EVICTION, "" + cacheRuleReader.getAdminAuthStats().evictionCount());

        statsValues.put(KEY_USER_SIZE, "" + cacheRuleReader.getUserCacheSize());
        statsValues.put(KEY_USER_HIT, "" + cacheRuleReader.getUserStats().hitCount());
        statsValues.put(KEY_USER_MISS, "" + cacheRuleReader.getUserStats().missCount());
        statsValues.put(KEY_USER_LOADOK, "" + cacheRuleReader.getUserStats().loadSuccessCount());
        statsValues.put(KEY_USER_LOADKO, "" + cacheRuleReader.getUserStats().loadExceptionCount());
        statsValues.put(KEY_USER_LOADTIME, "" + cacheRuleReader.getUserStats().totalLoadTime());
        statsValues.put(KEY_USER_EVICTION, "" + cacheRuleReader.getUserStats().evictionCount());
    }

    /** Creates a new wicket model from the configuration object. */
    private IModel<GeoFenceConfiguration> getGeoFenceConfigModel() {
        return new Model<GeoFenceConfiguration>(config);
    }

    /** Creates a new wicket model from the configuration object. */
    private IModel<CacheConfiguration> getCacheConfigModel() {
        return new Model<CacheConfiguration>(cacheParams);
    }
}
