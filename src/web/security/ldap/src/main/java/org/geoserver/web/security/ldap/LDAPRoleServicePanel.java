/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.util.Optional;
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

public class LDAPRoleServicePanel extends RoleServicePanel<LDAPRoleServiceConfig> {

    private static final long serialVersionUID = -67788557484913489L;

    private static final String USE_NESTED_PARENT_GROUPS = "useNestedParentGroups";
    private static final String MAX_GROUP_SEARCH_LEVEL = "maxGroupSearchLevel";
    private static final String NESTED_GROUP_SEARCH_FILTER = "nestedGroupSearchFilter";
    private static final String NESTED_SEARCH_FIELDS_CONTAINER = "nestedSearchFieldsContainer";

    class LDAPAuthenticationPanel extends FormComponentPanel<String> {

        /** serialVersionUID */
        private static final long serialVersionUID = 8919421089437979222L;

        public LDAPAuthenticationPanel(String id) {
            super(id, new Model<String>());
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

    public LDAPRoleServicePanel(String id, IModel<LDAPRoleServiceConfig> model) {
        super(id, model);
        add(new TextField<String>("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        add(new TextField<String>("groupSearchBase").setRequired(true));
        add(new TextField<String>("groupSearchFilter"));
        add(new TextField<String>("allGroupsSearchFilter"));
        add(new TextField<String>("userFilter"));
        add(
                new AjaxCheckBox("bindBeforeGroupSearch") {
                    private static final long serialVersionUID = -1675695153498067857L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        WebMarkupContainer c =
                                (WebMarkupContainer)
                                        LDAPRoleServicePanel.this.get(
                                                "authenticationPanelContainer");

                        // reset any values that were set
                        LDAPAuthenticationPanel ldapAuthenticationPanel =
                                (LDAPAuthenticationPanel) c.get("authenticationPanel");
                        ldapAuthenticationPanel.resetModel();
                        ldapAuthenticationPanel.setVisible(getModelObject().booleanValue());
                        target.add(c);
                    }
                });
        hierarchicalGroupsInit();

        LDAPAuthenticationPanel authPanel = new LDAPAuthenticationPanel("authenticationPanel");
        authPanel.setVisible(model.getObject().isBindBeforeGroupSearch());
        add(
                new WebMarkupContainer("authenticationPanelContainer")
                        .add(authPanel)
                        .setOutputMarkupId(true));
    }

    private void hierarchicalGroupsInit() {
        // hierarchical groups configurations
        final WebMarkupContainer nestedSearchFieldsContainer =
                new WebMarkupContainer(NESTED_SEARCH_FIELDS_CONTAINER);
        nestedSearchFieldsContainer.setOutputMarkupPlaceholderTag(true);
        nestedSearchFieldsContainer.setOutputMarkupId(true);
        add(nestedSearchFieldsContainer);
        Optional<LDAPRoleServiceConfig> useNestedOpt =
                Optional.of(this).map(x -> x.configModel).map(IModel::getObject);
        // get initial value for use_nested checkbox
        boolean useNestedActivated =
                useNestedOpt.map(LDAPRoleServiceConfig::isUseNestedParentGroups).orElse(false);
        nestedSearchFieldsContainer.setVisible(useNestedActivated);
        final AjaxCheckBox useNestedCheckbox =
                new AjaxCheckBox(USE_NESTED_PARENT_GROUPS) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        AjaxCheckBox cb =
                                (AjaxCheckBox)
                                        LDAPRoleServicePanel.this.get(USE_NESTED_PARENT_GROUPS);
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
