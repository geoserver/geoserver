/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.Serial;
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
import org.geoserver.geofence.cache.CacheManager;
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

/**
 * GeoFence wicket administration UI for GeoServer.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
// TODO WICKET8 - Verify this page works OK
public class GeofencePage extends GeoServerSecuredPage {

    @Serial
    private static final long serialVersionUID = 5845823599005718408L;

    /** Configuration object. */
    private GeoFenceConfiguration config;

    private CacheConfiguration cacheParams;

    public GeofencePage() {
        // extracts cfg object from the registered probe instance
        GeoFenceConfigurationManager configManager = GeoServerExtensions.bean(GeoFenceConfigurationManager.class);

        config = configManager.getConfiguration().clone();
        cacheParams = configManager.getCacheConfiguration().clone();

        final IModel<GeoFenceConfiguration> configModel = getGeoFenceConfigModel();
        final IModel<CacheConfiguration> cacheModel = getCacheConfigModel();
        Form<IModel<GeoFenceConfiguration>> form =
                new Form<>("form", new CompoundPropertyModel<IModel<GeoFenceConfiguration>>(configModel));
        form.setOutputMarkupId(true);
        add(form);
        form.add(new TextField<>("instanceName", new PropertyModel<>(configModel, "instanceName")).setRequired(true));
        // .setVisible(!config.isInternal());
        form.add(new TextField<>(
                        "servicesUrl",
                        new ExtPropertyModel<String>(configModel, "servicesUrl").setReadOnly(config.isInternal()))
                .setRequired(true)
                .setEnabled(!config.isInternal()));

        form.add(
                new AjaxSubmitLink("test") {
                    @Serial
                    private static final long serialVersionUID = -91239899377941223L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        ((FormComponent<?>) form.get("servicesUrl")).processInput();
                        String servicesUrl = (String) ((FormComponent<?>) form.get("servicesUrl")).getConvertedInput();

                        try {
                            RuleReaderService ruleReader = getRuleReaderService(servicesUrl);
                            ruleReader.getMatchingRules(new RuleFilter());

                            info(new StringResourceModel(GeofencePage.class.getSimpleName() + ".connectionSuccessful")
                                    .getObject());
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, e.getMessage(), e);
                        }

                        if (getPage() instanceof GeoServerBasePage) {
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        }
                    }
                    // HttpInvokerProxyFactoryBean is deprecated because
                    // Spring no longer supports serialized RMI invocations
                    @SuppressWarnings("deprecation")
                    private RuleReaderService getRuleReaderService(String servicesUrl) throws IOException {
                        if (config.isInternal()) {
                            return (RuleReaderService) GeoServerExtensions.bean("ruleReaderService");
                        } else {
                            /*org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean invoker =
                                    new org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean();
                            invoker.setServiceUrl(servicesUrl);
                            invoker.setServiceInterface(RuleReaderService.class);
                            invoker.afterPropertiesSet();
                            return (RuleReaderService) invoker.getObject();*/

                            return (RuleReaderService) null;
                        }
                    }
                }.setDefaultFormProcessing(false));

        form.add(new CheckBox(
                "allowRemoteAndInlineLayers", new PropertyModel<>(configModel, "allowRemoteAndInlineLayers")));
        form.add(new CheckBox(
                "grantWriteToWorkspacesToAuthenticatedUsers",
                new PropertyModel<>(configModel, "grantWriteToWorkspacesToAuthenticatedUsers")));
        form.add(new CheckBox("useRolesToFilter", new PropertyModel<>(configModel, "useRolesToFilter")));

        form.add(new TextField<>("acceptedRoles", new PropertyModel<>(configModel, "acceptedRoles")));

        Button submit = new Button("submit") {
            @Serial
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

        Button cancel = new Button("cancel") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        }.setDefaultFormProcessing(false);
        form.add(cancel);

        form.add(new TextField<>("cacheSize", new PropertyModel<>(cacheModel, "size")).setRequired(true));

        form.add(new TextField<>("cacheRefresh", new PropertyModel<>(cacheModel, "refreshMilliSec")).setRequired(true));

        form.add(new TextField<>("cacheExpire", new PropertyModel<>(cacheModel, "expireMilliSec")).setRequired(true));

        CacheManager cacheManager = GeoServerExtensions.bean(CacheManager.class);
        updateStatsValues(cacheManager);

        for (String key : statsValues.keySet()) {
            Label label = new Label(key, new MapModel<>(statsValues, key));
            label.setOutputMarkupId(true);
            form.add(label);
            statsLabels.add(label);
        }

        form.add(
                new AjaxSubmitLink("invalidate") {

                    @Serial
                    private static final long serialVersionUID = 3847903240475052867L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        CacheManager cacheManager = GeoServerExtensions.bean(CacheManager.class);
                        cacheManager.invalidateAll();
                        info(new StringResourceModel(GeofencePage.class.getSimpleName() + ".cacheInvalidated")
                                .getObject());
                        updateStatsValues(cacheManager);
                        for (Label label : statsLabels) {
                            target.add(label);
                        }

                        if (getPage() instanceof GeoServerBasePage) {
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        }
                    }
                }.setDefaultFormProcessing(false));
    }

    private final Map<String, Object> statsValues = new HashMap<>();

    private final Set<Label> statsLabels = new HashSet<>();

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

    private static final String KEY_CONT_SIZE = "cont.size";
    private static final String KEY_CONT_HIT = "cont.hit";
    private static final String KEY_CONT_MISS = "cont.miss";
    private static final String KEY_CONT_LOADOK = "cont.loadok";
    private static final String KEY_CONT_LOADKO = "cont.loadko";
    private static final String KEY_CONT_LOADTIME = "cont.loadtime";
    private static final String KEY_CONT_EVICTION = "cont.evict";

    private void updateStatsValues(CacheManager cacheManager) {

        LoadingCache cache = cacheManager.getRuleCache();
        statsValues.put(KEY_RULE_SIZE, "" + cache.size());
        statsValues.put(KEY_RULE_HIT, "" + cache.stats().hitCount());
        statsValues.put(KEY_RULE_MISS, "" + cache.stats().missCount());
        statsValues.put(KEY_RULE_LOADOK, "" + cache.stats().loadSuccessCount());
        statsValues.put(KEY_RULE_LOADKO, "" + cache.stats().loadExceptionCount());
        statsValues.put(KEY_RULE_LOADTIME, "" + cache.stats().totalLoadTime());
        statsValues.put(KEY_RULE_EVICTION, "" + cache.stats().evictionCount());

        cache = cacheManager.getAuthCache();
        statsValues.put(KEY_ADMIN_SIZE, "" + cache.size());
        statsValues.put(KEY_ADMIN_HIT, "" + cache.stats().hitCount());
        statsValues.put(KEY_ADMIN_MISS, "" + cache.stats().missCount());
        statsValues.put(KEY_ADMIN_LOADOK, "" + cache.stats().loadSuccessCount());
        statsValues.put(KEY_ADMIN_LOADKO, "" + cache.stats().loadExceptionCount());
        statsValues.put(KEY_ADMIN_LOADTIME, "" + cache.stats().totalLoadTime());
        statsValues.put(KEY_ADMIN_EVICTION, "" + cache.stats().evictionCount());

        cache = cacheManager.getUserCache();
        statsValues.put(KEY_USER_SIZE, "" + cache.size());
        statsValues.put(KEY_USER_HIT, "" + cache.stats().hitCount());
        statsValues.put(KEY_USER_MISS, "" + cache.stats().missCount());
        statsValues.put(KEY_USER_LOADOK, "" + cache.stats().loadSuccessCount());
        statsValues.put(KEY_USER_LOADKO, "" + cache.stats().loadExceptionCount());
        statsValues.put(KEY_USER_LOADTIME, "" + cache.stats().totalLoadTime());
        statsValues.put(KEY_USER_EVICTION, "" + cache.stats().evictionCount());

        cache = cacheManager.getContainerCache();
        statsValues.put(KEY_CONT_SIZE, "" + cache.size());
        statsValues.put(KEY_CONT_HIT, "" + cache.stats().hitCount());
        statsValues.put(KEY_CONT_MISS, "" + cache.stats().missCount());
        statsValues.put(KEY_CONT_LOADOK, "" + cache.stats().loadSuccessCount());
        statsValues.put(KEY_CONT_LOADKO, "" + cache.stats().loadExceptionCount());
        statsValues.put(KEY_CONT_LOADTIME, "" + cache.stats().totalLoadTime());
        statsValues.put(KEY_CONT_EVICTION, "" + cache.stats().evictionCount());
    }

    /** Creates a new wicket model from the configuration object. */
    private IModel<GeoFenceConfiguration> getGeoFenceConfigModel() {
        return new Model<>(config);
    }

    /** Creates a new wicket model from the configuration object. */
    private IModel<CacheConfiguration> getCacheConfigModel() {
        return new Model<>(cacheParams);
    }
}
