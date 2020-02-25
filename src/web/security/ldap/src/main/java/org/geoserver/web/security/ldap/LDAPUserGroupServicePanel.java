/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.util.Optional;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.ldap.LDAPUserGroupServiceConfig;
import org.geoserver.security.web.usergroup.UserGroupServicePanel;

/** @author Niels Charlier */
public class LDAPUserGroupServicePanel extends UserGroupServicePanel<LDAPUserGroupServiceConfig> {
    private static final long serialVersionUID = -5052166946618920800L;

    private static final String USE_NESTED_PARENT_GROUPS = "useNestedParentGroups";
    private static final String MAX_GROUP_SEARCH_LEVEL = "maxGroupSearchLevel";
    private static final String NESTED_GROUP_SEARCH_FILTER = "nestedGroupSearchFilter";
    private static final String NESTED_SEARCH_FIELDS_CONTAINER = "nestedSearchFieldsContainer";

    class LDAPAuthenticationPanel extends WebMarkupContainer {

        private static final long serialVersionUID = 6533128678666053350L;

        public LDAPAuthenticationPanel(String id) {
            super(id);
            add(new TextField<String>("user"));

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

    public LDAPUserGroupServicePanel(String id, IModel<LDAPUserGroupServiceConfig> model) {
        super(id, model);
        /** LDAP server parameters */
        add(new TextField<String>("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        /** group options */
        add(new TextField<String>("groupSearchBase").setRequired(true));
        add(new TextField<String>("groupNameAttribute"));
        add(new TextField<String>("groupFilter"));
        add(new TextField<String>("allGroupsSearchFilter"));
        /** membership options */
        add(new TextField<String>("groupSearchFilter"));
        add(new TextField<String>("groupMembershipAttribute"));
        /** user options */
        add(new TextField<String>("userSearchBase").setRequired(true));
        add(new TextField<String>("userNameAttribute"));
        add(new TextField<String>("userFilter"));
        add(new TextField<String>("allUsersSearchFilter"));
        add(new TextField<String>("populatedAttributes"));
        hierarchicalGroupsInit();

        /** privileged account for querying the LDAP server (if needed) */
        add(
                new AjaxCheckBox("bindBeforeGroupSearch") {
                    private static final long serialVersionUID = -6388847010436939988L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // reset any values that were set
                        LDAPAuthenticationPanel ldapAuthenticationPanel =
                                (LDAPAuthenticationPanel)
                                        LDAPUserGroupServicePanel.this.get("authenticationPanel");
                        ldapAuthenticationPanel.resetModel();
                        ldapAuthenticationPanel.setVisible(getModelObject().booleanValue());
                        target.add(ldapAuthenticationPanel);
                    }
                });
        LDAPAuthenticationPanel authPanel = new LDAPAuthenticationPanel("authenticationPanel");
        authPanel.setVisible(model.getObject().isBindBeforeGroupSearch());
        authPanel.setOutputMarkupPlaceholderTag(true);
        add(authPanel);
    }

    private void hierarchicalGroupsInit() {
        // hierarchical groups configurations
        // create fields objects
        final WebMarkupContainer nestedSearchFieldsContainer =
                new WebMarkupContainer(NESTED_SEARCH_FIELDS_CONTAINER);
        nestedSearchFieldsContainer.setOutputMarkupPlaceholderTag(true);
        nestedSearchFieldsContainer.setOutputMarkupId(true);
        add(nestedSearchFieldsContainer);
        Optional<LDAPUserGroupServiceConfig> useNestedOpt =
                Optional.of(this).map(x -> x.configModel).map(IModel::getObject);
        // get initial value for use_nested checkbox
        boolean useNestedActivated =
                useNestedOpt.map(LDAPUserGroupServiceConfig::isUseNestedParentGroups).orElse(false);
        nestedSearchFieldsContainer.setVisible(useNestedActivated);

        final AjaxCheckBox useNestedCheckbox =
                new AjaxCheckBox(USE_NESTED_PARENT_GROUPS) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        AjaxCheckBox cb =
                                (AjaxCheckBox)
                                        LDAPUserGroupServicePanel.this.get(
                                                USE_NESTED_PARENT_GROUPS);
                        boolean value = cb.getModelObject();
                        nestedSearchFieldsContainer.setVisible(value);
                        target.add(nestedSearchFieldsContainer);
                    }
                };
        add(useNestedCheckbox);
        nestedSearchFieldsContainer.add(new TextField<String>(MAX_GROUP_SEARCH_LEVEL));
        nestedSearchFieldsContainer.add(new TextField<String>(NESTED_GROUP_SEARCH_FILTER));
    }
}
