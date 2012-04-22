/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerPreAuthenticationFilter;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.HelpLink;

/**
 * Main menu page for authentication.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationPage extends AbstractSecurityPage {

    public AuthenticationPage() {
        initComponents();
    }

    void initComponents() {
        Form<SecurityManagerConfig> form = new Form("form", 
            new CompoundPropertyModel<SecurityManagerConfig>(getSecurityManager().getSecurityConfig()));
        add(form);

        //form.add(new CheckBox("anonymousAuth"));

        form.add(new AuthenticationFiltersPanel("authFilters"));
        form.add(new HelpLink("authFiltersHelp").setDialog(dialog));

        form.add(new AuthenticationProvidersPanel("authProviders"));
        form.add(new HelpLink("authProvidersHelp").setDialog(dialog));

        form.add(new AuthFilterChainPanel("filterChain", 
            new PropertyModel<GeoServerSecurityFilterChain>(form.getModel(), "filterChain")));
        form.add(new HelpLink("filterChainHelp").setDialog(dialog));

        form.add(new AuthenticationChainPanel("providerChain", form));
        form.add(new HelpLink("providerChainHelp").setDialog(dialog));

        form.add(new SubmitLink("save", form) {
            @Override
            public void onSubmit() {
                try {
                    getSecurityManager()
                        .saveSecurityConfig((SecurityManagerConfig) getForm().getModelObject());
                    doReturn();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error saving authentication config", e);
                    error(e);
                }
            }
        });
        form.add(new Link("cancel") {
            @Override
            public void onClick() {
                doReturn();
            }
        });
    }

    class AuthenticationChainPanel extends FormComponentPanel {

        public AuthenticationChainPanel(String id, Form form) {
            super(id, new Model());

            add(new AuthenticationChainPalette("authProviderNames"));
        }
    }

    class RequestChainDropDownChoice extends DropDownChoice<RequestFilterChain> {

        public RequestChainDropDownChoice(String id, IModel<RequestFilterChain> model, 
            IModel<List<RequestFilterChain>> choices) {
            super(id, model, choices, new ChoiceRenderer<RequestFilterChain>() {
                @Override
                public Object getDisplayValue(RequestFilterChain object) {
                    String name = object.getName();
                    return new ResourceModel(RequestFilterChain.class.getSimpleName()+"."+name,name).getObject();
                }
                @Override
                public String getIdValue(RequestFilterChain object, int index) {
                    return object.getName();
                }
            });
        }
    }

    class AuthFilterChainPanel extends FormComponentPanel {

        RequestFilterChain requestChain;

        public AuthFilterChainPanel(String id, IModel<GeoServerSecurityFilterChain> model) {
            super(id, new Model());

            requestChain = model.getObject().getRequestChainByName("web");
            add(new RequestChainDropDownChoice("requestChain", new PropertyModel(this, "requestChain"), 
                new PropertyModel<List<RequestFilterChain>>(model, "requestChains")).add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(AuthFilterChainPanel.this.get("authFilterChain"));
                }
            }));

            add(new AuthFilterChainPalette("authFilterChain", new AuthFilterNamesModel(model))
                .setOutputMarkupId(true)); 
        }

        class AuthFilterNamesModel implements IModel<List<String>> {

            IModel<GeoServerSecurityFilterChain> filterChainModel;

            AuthFilterNamesModel(IModel<GeoServerSecurityFilterChain> filterChainModel) {
                this.filterChainModel = filterChainModel;
            }

            @Override
            public List<String> getObject() {
                
                GeoServerSecurityManager secMgr = getSecurityManager();
                List<String> filters = new ArrayList(requestChain.getFilterNames());
                try {
                    filters.retainAll(secMgr.listFilters(GeoServerAuthenticationFilter.class));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return filters;
            }
    
            @Override
            public void setObject(List<String> object) {
                if (!requestChain.updateAuthFilters(object)) {
                    error("Unable to update filters for " + requestChain);
                }
            }
            
            @Override
            public void detach() {
                filterChainModel.detach();
            }
        }
    }
}
