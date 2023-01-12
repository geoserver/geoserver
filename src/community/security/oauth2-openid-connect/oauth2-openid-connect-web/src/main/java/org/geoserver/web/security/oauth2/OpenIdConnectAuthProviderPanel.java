/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.web.security.oauth2;

import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.AccessToken;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.IdToken;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.MSGraphAPI;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.UserInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.oauth2.DiscoveryClient;
import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OpenIdConnectAuthProviderPanel
        extends GeoServerOAuth2AuthProviderPanel<OpenIdConnectFilterConfig> {

    /**
     * If they have chosen MSGraphAPI as the RoleProvider, we need to make sure that the userinfo
     * endpoint is also an MS Graph URL. If not, they've probably made a misconfiguration - the
     * bearer token is from another IDP and this will cause issues access the MS graph endpoint.
     * Let's fail early.
     */
    class MSGraphRoleProviderOnlyWithMSGraphSystem extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return null;
        }

        @Override
        public void validate(Form<?> form) {
            DropDownChoice roleSource = (DropDownChoice) form.get("panel").get("roleSource");
            if (roleSource == null) {
                return;
            }
            if (!MSGraphAPI.equals(roleSource.getConvertedInput())) {
                return;
            }

            TextField userInfoTextField =
                    (TextField) form.get("panel").get("checkTokenEndpointUrl");

            String userInfoEndpointUrl = (String) userInfoTextField.getConvertedInput();

            if (!userInfoEndpointUrl.startsWith("https://graph.microsoft.com/")) {
                form.error(form.getString("OpenIdConnectAuthProviderPanel.invalidMSGraphURL"));
            }
        }
    }

    /**
     * Attached Bearer Tokens are NOT compatible with ID Tokens roles source. This is because there
     * isn't an ID Token available when the Access Token is attached to the HTTP request. User
     * should choose a different role Source (which may require setting up the IDP to put roles in a
     * different location).
     */
    class BearerTokenNoIDTokensValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return null;
        }

        /**
         * if Bearer Tokens are on, and role source is ID Token, then give an error message
         *
         * @param form
         */
        @Override
        public void validate(Form<?> form) {
            CheckBox allowBearerTokensCheckbox =
                    (CheckBox) form.get("panel").get("allowBearerTokens");
            if (allowBearerTokensCheckbox == null) {
                return; // this happens when the "discovery" button is pressed
            }
            if (!allowBearerTokensCheckbox.getConvertedInput())
                return; // bearer tokens not allowed -> no issues

            DropDownChoice roleSource = (DropDownChoice) form.get("panel").get("roleSource");

            if (IdToken.equals(roleSource.getConvertedInput())) {
                form.error(
                        form.getString("OpenIdConnectAuthProviderPanel.invalidBearerRoleSource"));
            }
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getForm().add(new BearerTokenNoIDTokensValidator());
        getForm().add(new MSGraphRoleProviderOnlyWithMSGraphSystem());
    }

    public OpenIdConnectAuthProviderPanel(String id, IModel<OpenIdConnectFilterConfig> model) {
        super(id, model);

        add(new HelpLink("principalKeyHelp", this).setDialog(dialog));
        add(new TextField<String>("principalKey"));

        add(new HelpLink("jwkURIHelp", this).setDialog(dialog));
        add(new TextField<String>("jwkURI"));

        add(new HelpLink("postLogoutRedirectUriHelp", this).setDialog(dialog));
        add(new TextField<String>("postLogoutRedirectUri"));

        add(new HelpLink("responseModeHelp", this).setDialog(dialog));
        add(new TextField<String>("responseMode"));
        add(new HelpLink("sendClientSecretHelp", this).setDialog(dialog));
        add(new CheckBox("sendClientSecret"));

        add(new HelpLink("allowUnSecureLoggingHelp", this).setDialog(dialog));
        add(new CheckBox("allowUnSecureLogging"));

        add(new HelpLink("allowBearerTokensHelp", this).setDialog(dialog));
        add(new CheckBox("allowBearerTokens"));
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (IdToken.equals(model) || AccessToken.equals(model) || UserInfo.equals(model)) {
            return new TokenClaimPanel("panel");
        }
        return super.getRoleSourcePanel(model);
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources = new ArrayList<>(Arrays.asList(OpenIdRoleSource.values()));
        sources.addAll(Arrays.asList(PreAuthenticatedUserNameRoleSource.values()));
        return new DropDownChoice<>("roleSource", sources, new RoleSourceChoiceRenderer());
    }

    static class TokenClaimPanel extends Panel {
        public TokenClaimPanel(String id) {
            super(id, new Model<>());
            add(new TextField<String>("tokenRolesClaim").setRequired(true));
        }
    }

    @Override
    protected Component getTopPanel(String panelId) {
        return new DiscoveryPanel(panelId);
    }

    private class DiscoveryPanel extends Panel {

        String discoveryURL;

        public DiscoveryPanel(String panelId) {
            super(panelId);

            TextField<String> url =
                    new TextField<>("discoveryURL", new PropertyModel<>(this, "discoveryURL"));
            add(url);
            add(
                    new AjaxButton("discover") {

                        @Override
                        protected void onError(AjaxRequestTarget target, Form<?> form) {
                            onSubmit(target, form);
                        }

                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            url.processInput();
                            discover(url.getInput(), target);
                        }
                    });
            add(new HelpLink("discoveryURLKeyHelp", this).setDialog(dialog));
        }

        private void discover(String discoveryURL, AjaxRequestTarget target) {
            OpenIdConnectFilterConfig model =
                    (OpenIdConnectFilterConfig)
                            OpenIdConnectAuthProviderPanel.this.getForm().getModelObject();
            try {
                new DiscoveryClient(discoveryURL).autofill(model);
                target.add(OpenIdConnectAuthProviderPanel.this);
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            } catch (Exception e) {
                error(new ParamResourceModel("discoveryError", this, e.getMessage()).getString());
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        }
    }
}
