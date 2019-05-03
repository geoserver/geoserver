/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;
import javax.naming.AuthenticationException;
import org.apache.wicket.WicketRuntimeException;
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
import org.geoserver.security.ldap.LDAPSecurityProvider;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.util.MapModel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Configuration panel for {@link LDAPAuthenticationProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthProviderPanel extends AuthenticationProviderPanel<LDAPSecurityServiceConfig> {

    private static final long serialVersionUID = 4772173006888418298L;

    public LDAPAuthProviderPanel(String id, IModel<LDAPSecurityServiceConfig> model) {
        super(id, model);

        add(new TextField<String>("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        add(new TextField<String>("userDnPattern"));
        add(new TextField<String>("userFilter"));
        add(new TextField<String>("userFormat"));

        boolean useLdapAuth = model.getObject().getUserGroupServiceName() == null;
        add(
                new AjaxCheckBox("useLdapAuthorization", new Model<Boolean>(useLdapAuth)) {

                    private static final long serialVersionUID = 2060279075143716273L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        WebMarkupContainer c =
                                (WebMarkupContainer)
                                        LDAPAuthProviderPanel.this.get(
                                                "authorizationPanelContainer");

                        // reset any values that were set
                        ((AuthorizationPanel) c.get("authorizationPanel")).resetModel();

                        // remove the old panel
                        c.remove("authorizationPanel");

                        // add the new panel
                        c.add(createAuthorizationPanel("authorizationPanel", getModelObject()));

                        target.add(c);
                    }
                });
        add(
                new WebMarkupContainer("authorizationPanelContainer")
                        .add(createAuthorizationPanel("authorizationPanel", useLdapAuth))
                        .setOutputMarkupId(true));

        add(new TestLDAPConnectionPanel("testCx"));
    }

    AuthorizationPanel createAuthorizationPanel(String id, boolean useLDAP) {
        return useLDAP ? new LDAPAuthorizationPanel(id) : new UserGroupAuthorizationPanel(id);
    }

    abstract class AuthorizationPanel extends FormComponentPanel<HashMap<String, Object>> {

        private static final long serialVersionUID = -2021795762927385164L;

        public AuthorizationPanel(String id) {
            super(id, new Model<HashMap<String, Object>>());
        }

        public abstract void resetModel();
    }

    class UserGroupAuthorizationPanel extends AuthorizationPanel {

        private static final long serialVersionUID = 2464048864034610244L;

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

        private static final long serialVersionUID = 7541432269535150812L;
        private static final String USE_NESTED_PARENT_GROUPS = "useNestedParentGroups";
        private static final String MAX_GROUP_SEARCH_LEVEL = "maxGroupSearchLevel";
        private static final String NESTED_GROUP_SEARCH_FILTER = "nestedGroupSearchFilter";
        private static final String NESTED_SEARCH_FIELDS_CONTAINER = "nestedSearchFieldsContainer";

        public LDAPAuthorizationPanel(String id) {
            super(id);
            setOutputMarkupId(true);
            add(new CheckBox("bindBeforeGroupSearch"));
            add(new TextField<String>("adminGroup"));
            add(new TextField<String>("groupAdminGroup"));
            add(new TextField<String>("groupSearchBase"));
            add(new TextField<String>("groupSearchFilter"));
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            hierarchicalGroupsinit();
        }

        private void hierarchicalGroupsinit() {
            // hierarchical groups configurations
            Optional<LDAPSecurityServiceConfig> useNestedOpt =
                    Optional.of(this)
                            .map(
                                    x -> {
                                        try {
                                            return x.getForm();
                                        } catch (WicketRuntimeException ex) {
                                            // no form
                                        }
                                        return null;
                                    })
                            .map(Form::getModel)
                            .map(IModel::getObject)
                            .filter(x -> x instanceof LDAPSecurityServiceConfig)
                            .map(x -> (LDAPSecurityServiceConfig) x);
            // get initial value for use_nested checkbox
            boolean useNestedActivated =
                    useNestedOpt
                            .map(LDAPSecurityServiceConfig::isUseNestedParentGroups)
                            .orElse(false);
            // create fields objects
            final WebMarkupContainer nestedSearchFieldsContainer =
                    new WebMarkupContainer(NESTED_SEARCH_FIELDS_CONTAINER);
            nestedSearchFieldsContainer.setOutputMarkupPlaceholderTag(true);
            nestedSearchFieldsContainer.setOutputMarkupId(true);
            nestedSearchFieldsContainer.setVisible(useNestedActivated);
            add(nestedSearchFieldsContainer);
            final TextField<String> maxLevelField = new TextField<String>(MAX_GROUP_SEARCH_LEVEL);
            final TextField<String> nestedGroupSearchFilterField =
                    new TextField<String>(NESTED_GROUP_SEARCH_FILTER);
            final AjaxCheckBox useNestedCheckbox =
                    new AjaxCheckBox(USE_NESTED_PARENT_GROUPS) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            // get the checkbox boolean value and set visibility for fields
                            AjaxCheckBox cb =
                                    (AjaxCheckBox)
                                            LDAPAuthorizationPanel.this.get(
                                                    USE_NESTED_PARENT_GROUPS);
                            boolean value = cb.getModelObject();
                            nestedSearchFieldsContainer.setVisible(value);
                            target.add(nestedSearchFieldsContainer);
                        }
                    };
            add(useNestedCheckbox);
            nestedSearchFieldsContainer.add(maxLevelField);
            nestedSearchFieldsContainer.add(nestedGroupSearchFilterField);
        }

        @Override
        public void resetModel() {
            get("bindBeforeGroupSearch").setDefaultModelObject(null);
            get("adminGroup").setDefaultModelObject(null);
            get("groupAdminGroup").setDefaultModelObject(null);
            get("groupSearchBase").setDefaultModelObject(null);
            get("groupSearchFilter").setDefaultModelObject(null);
            // hierarchical groups reset
            get(USE_NESTED_PARENT_GROUPS).setDefaultModelObject(false);
        }
    }

    class TestLDAPConnectionPanel extends FormComponentPanel<HashMap<String, Object>> {

        private static final long serialVersionUID = 5433983389877706266L;

        public TestLDAPConnectionPanel(String id) {
            super(id, new Model<HashMap<String, Object>>(new HashMap<String, Object>()));

            add(
                    new TextField<String>(
                            "username", new MapModel<String>(getModel().getObject(), "username")));
            add(
                    new PasswordTextField(
                                    "password",
                                    new MapModel<String>(getModel().getObject(), "password"))
                            .setRequired(false));
            add(
                    new AjaxSubmitLink("test") {

                        private static final long serialVersionUID = 2373404292655355758L;

                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            // since this is not a regular form submit we have to manually update
                            // models
                            // of form components we care about
                            ((FormComponent<?>) TestLDAPConnectionPanel.this.get("username"))
                                    .processInput();
                            ((FormComponent<?>) TestLDAPConnectionPanel.this.get("password"))
                                    .processInput();

                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("serverURL"))
                                    .processInput();
                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("useTLS"))
                                    .processInput();

                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("userDnPattern"))
                                    .processInput();
                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("userFilter"))
                                    .processInput();
                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("userFormat"))
                                    .processInput();

                            String username =
                                    (String)
                                            ((FormComponent<?>)
                                                            TestLDAPConnectionPanel.this.get(
                                                                    "username"))
                                                    .getConvertedInput();
                            String password =
                                    (String)
                                            ((FormComponent<?>)
                                                            TestLDAPConnectionPanel.this.get(
                                                                    "password"))
                                                    .getConvertedInput();

                            LDAPSecurityServiceConfig ldapConfig =
                                    (LDAPSecurityServiceConfig) getForm().getModelObject();
                            doTest(ldapConfig, username, password);

                            target.add(getPage().get("topFeedback"));
                        }

                        void doTest(
                                LDAPSecurityServiceConfig ldapConfig,
                                String username,
                                String password) {

                            try {

                                if (ldapConfig.getUserDnPattern() == null
                                        && ldapConfig.getUserFilter() == null) {
                                    error("Neither user dn pattern or user filter specified");
                                    return;
                                }

                                LDAPSecurityProvider provider =
                                        new LDAPSecurityProvider(getSecurityManager());
                                LDAPAuthenticationProvider authProvider =
                                        (LDAPAuthenticationProvider)
                                                provider.createAuthenticationProvider(ldapConfig);
                                Authentication authentication =
                                        authProvider.authenticate(
                                                new UsernamePasswordAuthenticationToken(
                                                        username, password));
                                if (authentication == null || !authentication.isAuthenticated()) {
                                    throw new AuthenticationException(
                                            "Cannot authenticate " + username);
                                }

                                provider.destroy(null);
                                info(
                                        new StringResourceModel(
                                                        LDAPAuthProviderPanel.class.getSimpleName()
                                                                + ".connectionSuccessful")
                                                .getObject());
                            } catch (Exception e) {
                                error(e);
                                LOGGER.log(Level.WARNING, e.getMessage(), e);
                            }
                        }
                    }.setDefaultFormProcessing(false));
        }
    }
}
