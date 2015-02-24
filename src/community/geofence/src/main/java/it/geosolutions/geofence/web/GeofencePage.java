/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geofence.web;

import it.geosolutions.geofence.GeofenceAccessManager;
import it.geosolutions.geofence.config.GeoFenceConfiguration;
import it.geosolutions.geofence.cache.CacheConfiguration;
import it.geosolutions.geofence.cache.CachedRuleReader;
import it.geosolutions.geofence.services.RuleReaderService;
import it.geosolutions.geofence.services.dto.RuleFilter;
import it.geosolutions.geofence.services.dto.ShortRule;

import java.util.List;
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
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import com.google.common.cache.CacheStats;
import it.geosolutions.geofence.config.GeoFenceConfigurationController;
import it.geosolutions.geofence.config.GeoFenceConfigurationManager;

/**
 * GeoFence wicket administration UI for GeoServer.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class GeofencePage extends GeoServerSecuredPage {

    
    /**
     * Configuration object.
     */
    private GeoFenceConfiguration config;
    
    private CacheConfiguration cacheParams;
    
    public GeofencePage() {
        // extracts cfg object from the registered probe instance
        GeoFenceConfigurationManager configManager = GeoServerExtensions.bean(GeoFenceConfigurationManager.class);

        config = configManager.getConfiguration().clone();
        cacheParams = configManager.getCacheConfiguration().clone();
        
    
        final IModel<GeoFenceConfiguration> configModel = getGeoFenceConfigModel();
        final IModel<CacheConfiguration> cacheModel = getCacheConfigModel();
        Form<IModel<GeoFenceConfiguration>> form = new Form<IModel<GeoFenceConfiguration>>(
                "form",
                new CompoundPropertyModel<IModel<GeoFenceConfiguration>>(
                        configModel));
        form.setOutputMarkupId(true);
        add(form);
    
        form.add(new TextField<String>("instanceName", 
                new PropertyModel<String>(configModel, "instanceName")).setRequired(true));
        form.add(new TextField<String>("servicesUrl", 
                new PropertyModel<String>(configModel, "servicesUrl")).setRequired(true));
    
        form.add(new AjaxSubmitLink("test") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                ((FormComponent)form.get("servicesUrl")).processInput();
                String servicesUrl = (String)((FormComponent)form.get("servicesUrl")).getConvertedInput();
                RuleReaderService ruleReader = getRuleReaderService(servicesUrl);
                try {
                    List<ShortRule> rules = ruleReader.getMatchingRules(new RuleFilter());
                    
                    info(new StringResourceModel(GeofencePage.class.getSimpleName() + 
                            ".connectionSuccessful", null).getObject());
                } catch(Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
                
                target.addComponent(getPage().get("feedback"));
            }

            private RuleReaderService getRuleReaderService(String servicesUrl) {
                HttpInvokerProxyFactoryBean invoker = new org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean();
                invoker.setServiceUrl(servicesUrl);
                invoker.setServiceInterface(RuleReaderService.class);
                invoker.afterPropertiesSet();
                return (RuleReaderService)invoker.getObject();
            }
        }.setDefaultFormProcessing(false));
        
        form.add(new CheckBox("allowRemoteAndInlineLayers",
                new PropertyModel<Boolean>(configModel,
                        "allowRemoteAndInlineLayers")));
        form.add(new CheckBox("allowDynamicStyles", new PropertyModel<Boolean>(
                configModel, "allowDynamicStyles")));
        form.add(new CheckBox("grantWriteToWorkspacesToAuthenticatedUsers",
                new PropertyModel<Boolean>(configModel,
                        "grantWriteToWorkspacesToAuthenticatedUsers")));
        form.add(new CheckBox("useRolesToFilter", new PropertyModel<Boolean>(
                configModel, "useRolesToFilter")));
    
        form.add(new TextField<String>("acceptedRoles", new PropertyModel<String>(
                configModel, "acceptedRoles")));
    
        Button submit = new Button("submit", new StringResourceModel("submit",
                this, null)) {
            private static final long serialVersionUID = 1L;
    
            @Override
            public void onSubmit() {
                try {
                    // save the changed configuration
                    GeoServerExtensions
                            .bean(GeoFenceConfigurationController.class)
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
            private static final long serialVersionUID = 1L;
    
            @Override
            public void onSubmit() {
                doReturn();
            }
        }.setDefaultFormProcessing(false);
        form.add(cancel);
        
        
        
        

        form.add(new TextField<Long>("cacheSize", new PropertyModel<Long>(
                cacheModel, "size")).setRequired(true));
        
        form.add(new TextField<Long>("cacheRefresh", new PropertyModel<Long>(
                cacheModel, "refreshMilliSec")).setRequired(true));
        
        form.add(new TextField<Long>("cacheExpire", new PropertyModel<Long>(
                cacheModel, "expireMilliSec")).setRequired(true));


        CachedRuleReader cacheRuleReader = GeoServerExtensions.bean(CachedRuleReader.class);

        final Model<String> ruleStatsModel = new Model(getStats(cacheRuleReader));
        final Label ruleStats = new Label("rulestats", ruleStatsModel);
        ruleStats.setOutputMarkupId(true); 
        ruleStats.setEscapeModelStrings(false);
        form.add(ruleStats);
        final Model<String> userStatsModel = new Model(getUserStats(cacheRuleReader));
        final Label userStats = new Label("userstats", userStatsModel);
        userStats.setEscapeModelStrings(false);
        userStats.setOutputMarkupId(true);
        form.add(userStats);
        
        form.add(new AjaxSubmitLink("invalidate") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                CachedRuleReader cacheRuleReader = GeoServerExtensions
                    .bean(CachedRuleReader.class);
                cacheRuleReader.invalidateAll();
                info(new StringResourceModel(GeofencePage.class.getSimpleName() + 
                        ".cacheInvalidated", null).getObject());
                ruleStatsModel.setObject(getStats(cacheRuleReader));
                userStatsModel.setObject(getUserStats(cacheRuleReader));
                target.addComponent(ruleStats);
                target.addComponent(userStats);
                target.addComponent(getPage().get("feedback"));
            }

            
            
        }.setDefaultFormProcessing(false));
        
        
    }
    
    private String getStats(CachedRuleReader cacheRuleReader) {
        CacheStats stats = cacheRuleReader.getStats();
        return new StringBuilder()
                .append("<b>RuleStats</b><ul>")
                .append("<li>size: ").append(cacheRuleReader.getCacheSize())
                    .append("/")
                    .append(cacheRuleReader.getCacheInitParams().getSize()).append("</li>")
                .append("<li>hitCount: ").append(stats.hitCount()).append("</li>")
                .append("<li>missCount: ").append(stats.missCount()).append("</li>")
                .append("<li>loadSuccessCount: ").append(stats.loadSuccessCount()).append("</li>")
                .append("<li>loadExceptionCount: ").append(stats.loadExceptionCount()).append("</li>")
                .append("<li>totalLoadTime: ").append(stats.totalLoadTime()).append("</li>")
                .append("<li>evictionCount: ").append(stats.evictionCount()).append("</li>")
                .append("</ul>").toString();
        
    }

    private String getUserStats(CachedRuleReader cacheRuleReader) {
        CacheStats stats;
        StringBuilder sb;
        stats = cacheRuleReader.getUserStats();
        sb = new StringBuilder().append("<b>UserStats</b><ul>")
                .append("<li>size: ").append(cacheRuleReader.getUserCacheSize())
                    .append("/")
                    .append(cacheRuleReader.getCacheInitParams().getSize()).append("</li>")
                .append("<li>hitCount: ").append(stats.hitCount()).append("</li>")
                .append("<li>missCount: ").append(stats.missCount()).append("</li>")
                .append("<li>loadSuccessCount: ").append(stats.loadSuccessCount()).append("</li>")
                .append("<li>loadExceptionCount: ").append(stats.loadExceptionCount()).append("</li>")
                .append("<li>totalLoadTime: ").append(stats.totalLoadTime()).append("</li>")
                .append("<li>evictionCount: ").append(stats.evictionCount()).append("</li>")
                .append("</ul>");
        return sb.toString();
    }

    /**
     * Creates a new wicket model from the configuration object.
     * 
     * @return
     */
    private IModel<GeoFenceConfiguration> getGeoFenceConfigModel() {
        return new Model<GeoFenceConfiguration>(config);
    }
    
    /**
     * Creates a new wicket model from the configuration object.
     * 
     * @return
     */
    private IModel<CacheConfiguration> getCacheConfigModel() {
        return new Model<CacheConfiguration>(cacheParams);
    }


}
