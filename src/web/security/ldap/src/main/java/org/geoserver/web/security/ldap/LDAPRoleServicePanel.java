/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.ldap.LDAPRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanel;
import org.geoserver.web.security.ldap.LDAPAuthProviderPanel.AuthorizationPanel;
import org.geoserver.web.security.ldap.LDAPAuthProviderPanel.LDAPAuthorizationPanel;
import org.geoserver.web.security.ldap.LDAPAuthProviderPanel.UserGroupAuthorizationPanel;

public class LDAPRoleServicePanel extends RoleServicePanel<LDAPRoleServiceConfig> {

    
        class LDAPAuthenticationPanel extends FormComponentPanel {
        
            public LDAPAuthenticationPanel(String id) {
                super(id, new Model());
                add(new TextField("user"));
            
                PasswordTextField pwdField = new PasswordTextField("password");
                // avoid reseting the password which results in an
                // empty password on saving a modified configuration
                pwdField.setResetPassword(false);
                add(pwdField);
            }
            
            public void resetModel() {
                get("user").setDefaultModelObject(null);
                get("password").setDefaultModelObject(null);
            }
        }
    
    public LDAPRoleServicePanel(String id, IModel<LDAPRoleServiceConfig> model) {
        super(id, model);
        add(new TextField("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        add(new TextField("groupSearchBase").setRequired(true));
        add(new TextField("groupSearchFilter"));
        add(new TextField("allGroupsSearchFilter"));
        add(new TextField("userFilter"));
        add(new AjaxCheckBox("bindBeforeGroupSearch") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                WebMarkupContainer c = (WebMarkupContainer) 
                        LDAPRoleServicePanel.this.get("authenticationPanelContainer");

                //reset any values that were set
                LDAPAuthenticationPanel ldapAuthenticationPanel = (LDAPAuthenticationPanel)c.get("authenticationPanel");
                ldapAuthenticationPanel.resetModel();
                ldapAuthenticationPanel.setVisible(getModelObject().booleanValue());
                target.addComponent(c);
            }
        });
        LDAPAuthenticationPanel authPanel = new LDAPAuthenticationPanel("authenticationPanel");
        authPanel.setVisible(model.getObject().isBindBeforeGroupSearch());
        add(new WebMarkupContainer("authenticationPanelContainer")
            .add(authPanel).setOutputMarkupId(true));
    }
}
