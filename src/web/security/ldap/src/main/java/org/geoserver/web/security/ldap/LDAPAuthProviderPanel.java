/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.AuthenticationException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.ldap.GeoserverLdapBindAuthenticator;
import org.geoserver.security.ldap.LDAPAuthenticationProvider;
import org.geoserver.security.ldap.LDAPSecurityProvider;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.util.MapModel;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.SpringSecurityAuthenticationSource;

/**
 * Configuration panel for {@link LDAPAuthenticationProvider}.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthProviderPanel extends AuthenticationProviderPanel<LDAPSecurityServiceConfig> {

    public LDAPAuthProviderPanel(String id, IModel<LDAPSecurityServiceConfig> model) {
        super(id, model);

        add(new TextField("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));        
        add(new TextField("userDnPattern"));
        add(new TextField("userFilter"));
        add(new TextField("userFormat"));

        boolean useLdapAuth = model.getObject().getUserGroupServiceName() == null;
        add(new AjaxCheckBox("useLdapAuthorization", new Model(useLdapAuth)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                WebMarkupContainer c = (WebMarkupContainer) 
                    LDAPAuthProviderPanel.this.get("authorizationPanelContainer");

                //reset any values that were set
                ((AuthorizationPanel)c.get("authorizationPanel")).resetModel();

                //remove the old panel
                c.remove("authorizationPanel");
                
                //add the new panel
                c.add(createAuthorizationPanel("authorizationPanel", getModelObject()));
                
                target.addComponent(c);
            }
        });
        add(new WebMarkupContainer("authorizationPanelContainer")
            .add(createAuthorizationPanel("authorizationPanel", useLdapAuth)).setOutputMarkupId(true));

        add(new TestLDAPConnectionPanel("testCx"));

    }

    AuthorizationPanel createAuthorizationPanel(String id, boolean useLDAP) {
        return useLDAP ? new LDAPAuthorizationPanel(id) : new UserGroupAuthorizationPanel(id);
    }

    abstract class AuthorizationPanel extends FormComponentPanel {

        public AuthorizationPanel(String id) {
            super(id, new Model());
        }

        public abstract void resetModel();
    }

    class UserGroupAuthorizationPanel extends AuthorizationPanel {

        public UserGroupAuthorizationPanel(String id) {
            super(id);

            add(new UserGroupServiceChoice("userGroupServiceName"));
        }

        @Override
        public void resetModel() {
            get("userGroupServiceName").setDefaultModelObject(null);
        }
    }

    class LDAPAuthorizationPanel extends AuthorizationPanel {

        public LDAPAuthorizationPanel(String id) {
            super(id);
            add(new CheckBox("bindBeforeGroupSearch"));
            add(new TextField("adminGroup"));
            add(new TextField("groupAdminGroup"));
            add(new TextField("groupSearchBase"));
            add(new TextField("groupSearchFilter"));
        }

        @Override
        public void resetModel() {
        	get("bindBeforeGroupSearch").setDefaultModelObject(null);
        	get("adminGroup").setDefaultModelObject(null);
        	get("groupAdminGroup").setDefaultModelObject(null);
            get("groupSearchBase").setDefaultModelObject(null);
            get("groupSearchFilter").setDefaultModelObject(null);
        }
    }

    class TestLDAPConnectionPanel extends FormComponentPanel {

        public TestLDAPConnectionPanel(String id) {
            super(id, new Model(new HashMap()));

            add(new TextField("username", new MapModel(getModel(), "username")));
            add(new PasswordTextField("password", new MapModel(getModel(), "password")).setRequired(false));
            add(new AjaxSubmitLink("test") {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    //since this is not a regular form submit we have to manually update models 
                    // of form components we care about
                    ((FormComponent)TestLDAPConnectionPanel.this.get("username")).processInput();
                    ((FormComponent)TestLDAPConnectionPanel.this.get("password")).processInput();

                    ((FormComponent)LDAPAuthProviderPanel.this.get("serverURL")).processInput();
                    ((FormComponent)LDAPAuthProviderPanel.this.get("useTLS")).processInput();

                    ((FormComponent)LDAPAuthProviderPanel.this.get("userDnPattern")).processInput();
                    ((FormComponent)LDAPAuthProviderPanel.this.get("userFilter")).processInput();
                    ((FormComponent)LDAPAuthProviderPanel.this.get("userFormat")).processInput();
                    
                    String username = (String)((FormComponent)TestLDAPConnectionPanel.this.get("username")).getConvertedInput();
                    String password = (String)((FormComponent)TestLDAPConnectionPanel.this.get("password")).getConvertedInput();
                    
                    LDAPSecurityServiceConfig ldapConfig = (LDAPSecurityServiceConfig) getForm().getModelObject();
                    doTest(ldapConfig, username, password);

                    target.addComponent(getPage().get("feedback"));
                }

                void doTest(LDAPSecurityServiceConfig ldapConfig, String username,
                        String password) {

                    
                    try {
                        
                        if (ldapConfig.getUserDnPattern() == null && ldapConfig.getUserFilter() == null) {
                            error("Neither user dn pattern or user filter specified");
                            return;
                        }
                        
                        LDAPSecurityProvider provider = new LDAPSecurityProvider(getSecurityManager());
                        LDAPAuthenticationProvider authProvider = (LDAPAuthenticationProvider) provider
                                .createAuthenticationProvider(ldapConfig);
                        Authentication authentication = authProvider
                                .authenticate(new UsernamePasswordAuthenticationToken(
                                        username, password));
                        if(authentication == null || !authentication.isAuthenticated()) {
                            throw new AuthenticationException("Cannot authenticate " + username);
                        }

                        provider.destroy(null);
                        info(new StringResourceModel(LDAPAuthProviderPanel.class.getSimpleName() + 
                            ".connectionSuccessful", null).getObject());
                    } catch (Exception e) {
                        error(e);
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                    finally {
                        
                    }
                    
                }
                
            }.setDefaultFormProcessing(false));
        }
    }
}
