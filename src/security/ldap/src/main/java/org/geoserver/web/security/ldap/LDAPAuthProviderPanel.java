/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.net.URI;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

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
import org.geoserver.security.ldap.LDAPAuthenticationProvider;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.util.MapModel;

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

            add(new TextField("groupSearchBase"));
            add(new TextField("groupSearchFilter"));
        }

        @Override
        public void resetModel() {
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
                    
                    Map map = (Map) TestLDAPConnectionPanel.this.getDefaultModelObject();
                    String username = (String) map.get("username");
                    String password = (String) map.get("password");
                    
                    LDAPSecurityServiceConfig ldapConfig = (LDAPSecurityServiceConfig) getForm().getModelObject();
                    doTest(ldapConfig, username, password);

                    target.addComponent(((GeoServerBasePage)getPage()).get("feedback"));
                }

                void doTest(LDAPSecurityServiceConfig ldapConfig, String username,
                        String password) {

                    LdapContext ctx = null;
                    try {
                        Hashtable env = new Hashtable(11);
                        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    
                        // Must use the name of the server that is found in its certificate
                        env.put(Context.PROVIDER_URL, ldapConfig.getServerURL());
    
                        env.put(Context.SECURITY_AUTHENTICATION, "simple");
                        URI url = new URI(ldapConfig.getServerURL());

                        if (ldapConfig.getUserDnPattern() == null) {
                            error("No user dn pattern specified");
                            return;
                        }
                        
                        String p = ldapConfig.getUserDnPattern().replaceAll("\\{0\\}", username) + "," + 
                                url.getPath().substring(1);
                        env.put(Context.SECURITY_PRINCIPAL, p);
                        env.put(Context.SECURITY_CREDENTIALS, password);

                        ctx = new InitialLdapContext(env, null);

                        info(new StringResourceModel(LDAPAuthProviderPanel.class.getSimpleName() + 
                            ".connectionSuccessful", null).getObject());
                    } catch (Exception e) {
                        error(e);
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                    finally {
                        if (ctx != null) {
                            try {
                                ctx.close();
                            } catch (NamingException e) {
                            }
                        }
                    }
                    
                }
                
            }.setDefaultFormProcessing(false));
        }
    }
}
