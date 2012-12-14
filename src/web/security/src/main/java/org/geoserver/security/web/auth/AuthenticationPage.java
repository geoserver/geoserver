/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.HelpLink;
import org.springframework.util.StringUtils;

/**
 * Main menu page for authentication.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationPage extends AbstractSecurityPage {

    Form<SecurityManagerConfig> form;
    LogoutFilterConfig logoutFilterConfig;
    
    public AuthenticationPage() {
        initComponents();
    }

    void initComponents() {
        
        // The request filter chain objects have to be cloned
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        List<RequestFilterChain> clones = new ArrayList<RequestFilterChain>();
        
        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {            
            try {
                clones.add((RequestFilterChain)chain.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }            
        }
        config.setFilterChain(new GeoServerSecurityFilterChain(clones));
        
        form = new Form("form", 
            new CompoundPropertyModel<SecurityManagerConfig>(config));
        add(form);

        try {
            logoutFilterConfig= (LogoutFilterConfig) getSecurityManager().loadFilterConfig(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        form.add(new TextField<String>("redirectURL",new PropertyModel<String>(this, "logoutFilterConfig.redirectURL")));

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
                    getSecurityManager().saveFilter(logoutFilterConfig);
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

    class RequestChainDropDownChoice extends DropDownChoice<VariableFilterChain> {

        public RequestChainDropDownChoice(String id, IModel<VariableFilterChain> model, 
            IModel<List<VariableFilterChain>> choices) {
            super(id, model, choices, new ChoiceRenderer<VariableFilterChain>() {
                @Override
                public Object getDisplayValue(VariableFilterChain object) {
                    String name = object.getName();
                    return new ResourceModel(RequestFilterChain.class.getSimpleName()+"."+name,name).getObject();
                }
                @Override
                public String getIdValue(VariableFilterChain object, int index) {
                    return object.getName();
                }
            });
        }
    }

    class AuthFilterChainPanel extends FormComponentPanel {

        VariableFilterChain requestChain;
        String antPatterns;
        AuthFilterChainPalette palette;
        RequestChainDropDownChoice dropDownChoice;
        DropDownChoice<String> roleFilterChoice;
        CheckBox disabled,allowSessionCreation;
        TextField<String> antPatternField;

        public AuthFilterChainPanel(String id, IModel<GeoServerSecurityFilterChain> model) {
            super(id, new Model());

            this.setOutputMarkupId(true);
            setRequestChain((VariableFilterChain) model.getObject().getRequestChainByName("web"));
            dropDownChoice=new RequestChainDropDownChoice("requestChain", new PropertyModel(this, "requestChain"), 
                    new PropertyModel<List<VariableFilterChain>>(model, "variableRequestChains"));
            add(dropDownChoice);
            dropDownChoice.add(
                new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    palette.setChain(dropDownChoice.getModelObject());
                    target.addComponent(roleFilterChoice);
                    target.addComponent(palette);
                    target.addComponent(disabled);
                    target.addComponent(allowSessionCreation);
                    target.addComponent(antPatternField);
                    //target.addComponent(AuthFilterChainPanel.this);
                }
                    
            }); 
            
            add(antPatternField=new TextField<String>("antPatterns",new PropertyModel<String>(this,"antPatterns")));
            antPatternField.setEnabled(false);
            antPatternField.setOutputMarkupId(true);
                    
            add(allowSessionCreation=new CheckBox("allowSessionCreation",new PropertyModel<Boolean>(this,"requestChain.allowSessionCreation")));
            allowSessionCreation.setOutputMarkupId(true);
            allowSessionCreation.add(
                    new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                    }                        
                });              
            
            add(disabled=new CheckBox("disabled",new PropertyModel<Boolean>(this,"requestChain.disabled")));
            disabled.setOutputMarkupId(true);
            disabled.add(
                    new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                       palette.setEnabled(!requestChain.isDisabled());
                       roleFilterChoice.setEnabled(!requestChain.isDisabled());
                       target.addComponent(palette);
                       target.addComponent(roleFilterChoice);
                    }                        
                });              

            
            add(palette=new AuthFilterChainPalette("authFilterChain", new AuthFilterNamesModel(model)));            
            palette.setOutputMarkupId(true);
            palette.setChain(requestChain);
            // the OnChangeAjaxBehavior is added in the AuthFilterChainPalette class  
            
            
            List<String> roleFilterNames=null;
            try {
                roleFilterNames = new ArrayList<String>(
                        getSecurityManager().listFilters(GeoServerRoleFilter.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }            
            add(roleFilterChoice=new DropDownChoice<String>("roleFilterChoice",
                    new PropertyModel<String>(this,"requestChain.roleFilterName"),
                    roleFilterNames));
            roleFilterChoice.setOutputMarkupId(true);
            roleFilterChoice.setNullValid(true);
            roleFilterChoice.add(
                    new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {                        
                    }                        
                });              
        }
        
        

        class AuthFilterNamesModel implements IModel<List<String>> {

            IModel<GeoServerSecurityFilterChain> filterChainModel;

            AuthFilterNamesModel(IModel<GeoServerSecurityFilterChain> filterChainModel) {
                this.filterChainModel = filterChainModel;
            }

            @Override
            public List<String> getObject() {
                
                GeoServerSecurityManager secMgr = getSecurityManager();
                List<String> filters = new ArrayList<String>(requestChain.getFilterNames());
                try {
                    filters.retainAll(requestChain.listFilterCandidates(secMgr));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return filters;
            }
    
            @Override
            public void setObject(List<String> object) {
                requestChain.setFilterNames(object);
            }
            
            @Override
            public void detach() {
                filterChainModel.detach();
            }
        }

        public VariableFilterChain getRequestChain() {
            return requestChain;
        }

        public void setRequestChain(VariableFilterChain requestChain) {            
            this.requestChain = requestChain;
            this.antPatterns=StringUtils.collectionToDelimitedString(requestChain.getPatterns(), "  ");            
        }
    }
}
