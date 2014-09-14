/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AuthenticationKeyFilterConfig;
import org.geoserver.security.AuthenticationKeyMapper;
import org.geoserver.security.GeoServerAuthenticationKeyFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerAuthenticationKeyFilter}.
 * 
 * @author mcr
 */
public class AuthenticationKeyFilterPanel 
    extends AuthenticationFilterPanel<AuthenticationKeyFilterConfig>  {

    
    private static final long serialVersionUID = 1;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    GeoServerDialog dialog;

    IModel<AuthenticationKeyFilterConfig> model;
    

    public AuthenticationKeyFilterPanel(String id, IModel<AuthenticationKeyFilterConfig> model) {
        super(id, model);
                
        dialog = (GeoServerDialog) get("dialog");
        this.model=model;
                        
        
        add(new HelpLink("authKeyParametersHelp",this).setDialog(dialog));
                
        
        add(new TextField<String>("authKeyParamName"));
        add(new AuthenticationKeyMapperChoice("authKeyMapperName"));
        add(new UserGroupServiceChoice("userGroupServiceName"));

        
        add(new AjaxSubmitLink("synchronize") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {                    
                    //AuthenticationKeyFilterPanel.this.updateModel();
                    AuthenticationKeyFilterConfig config = AuthenticationKeyFilterPanel.this.model.getObject();
                    
                    getSecurityManager().saveFilter(config);
                    AuthenticationKeyMapper mapper = (AuthenticationKeyMapper) GeoServerExtensions.bean(config.getAuthKeyMapperName());
                    mapper.setSecurityManager(getSecurityManager());
                    mapper.setUserGroupServiceName(config.getUserGroupServiceName());
                    int numberOfNewKeys=mapper.synchronize();
                    info(new StringResourceModel("synchronizeSuccessful",AuthenticationKeyFilterPanel.this, null,new Object[] {numberOfNewKeys}).getObject());
                }
                catch(Exception e) {
                    error(e);                    
                    LOGGER.log(Level.WARNING, "Authentication key  error ", e);
                }
                finally {
                    target.addComponent(getPage().get("feedback"));                    
                }

            }
        }.setDefaultFormProcessing(true));
        
    }
           
}
