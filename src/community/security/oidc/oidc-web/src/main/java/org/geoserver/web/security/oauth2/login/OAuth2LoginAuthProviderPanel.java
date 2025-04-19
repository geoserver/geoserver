/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.AccessToken;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.IdToken;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.UserInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2LoginAuthProviderPanel
        extends PreAuthenticatedUserNameFilterPanel<GeoServerOAuth2LoginFilterConfig> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3025321797363970333L;

    /** Prefix of Microsoft specific attributes */
    private static final String PREFIX_MS = "ms";

    /** Prefix of GitHub specific attributes */
    private static final String PREFIX_GIT_HUB = "gitHub";

    /** Prefix of Google specific attributes */
    private static final String PREFIX_GOOGLE = "google";

    /** Prefix of custom OIDC specific attributes */
    private static final String PREFIX_OIDC = "oidc";

    private class DiscoveryPanel extends Panel {
        private static final long serialVersionUID = 1L;

        public DiscoveryPanel(String panelId) {
            super(panelId);

            TextField<String> url = new TextField<>(
                    "oidcDiscoveryUri", new PropertyModel<>(configModel.getObject(), "oidcDiscoveryUri"));
            add(url);
            add(new AjaxButton("discover") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onError(AjaxRequestTarget target) {
                    onSubmit(target);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    url.processInput();
                    discover(url.getInput(), target);
                }
            });
            add(new HelpLink("oidcDiscoveryUriKeyHelp", this).setDialog(dialog));
        }

        private void discover(String discoveryURL, AjaxRequestTarget target) {
            GeoServerOAuth2LoginFilterConfig model = (GeoServerOAuth2LoginFilterConfig)
                    OAuth2LoginAuthProviderPanel.this.getForm().getModelObject();
            try {
                new DiscoveryClient(discoveryURL).autofill(model);
                target.add(OAuth2LoginAuthProviderPanel.this);
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            } catch (Exception e) {
                error(new ParamResourceModel("discoveryError", this, e.getMessage()).getString());
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        }
    }

    static class TokenClaimPanel extends Panel {
        private static final long serialVersionUID = 1L;

        public TokenClaimPanel(String id) {
            super(id, new Model<>());
            add(new TextField<String>("tokenRolesClaim").setRequired(true));
        }
    }

    private GeoServerDialog dialog;
    private List<Component> redirectUriComponents = new ArrayList<>();

    @SuppressWarnings("serial")
    public OAuth2LoginAuthProviderPanel(String id, IModel<GeoServerOAuth2LoginFilterConfig> model) {
        super(id, model);

        this.dialog = (GeoServerDialog) get("dialog");

        add(new HelpLink("userNameAttributeHelp", this).setDialog(dialog));

        add(new HelpLink("geoserverParametersHelp", this).setDialog(dialog));
        TextField<String> tf = new TextField<>("baseRedirectUri");

        add(tf);
        add(new HelpLink("baseRedirectUriHelp", this).setDialog(dialog));

        RepeatingView prefixView = new RepeatingView("pfv");
        add(prefixView);

        addProviderComponents(prefixView, PREFIX_GOOGLE, "Google");
        addProviderComponents(prefixView, PREFIX_GIT_HUB, "GitHub");
        addProviderComponents(prefixView, PREFIX_MS, "Microsoft Azure");
        addProviderComponents(prefixView, PREFIX_OIDC, "OpenID Connect Provider");

        tf.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget pTarget) {
                configModel.getObject().calculateRedirectUris();
                redirectUriComponents.forEach(c -> {
                    String lid = c.getMarkupId();
                    pTarget.add(c, lid);
                });
            }
        });

        add(new HelpLink("enableRedirectAuthenticationEntryPointHelp", this).setDialog(dialog));
        add(new CheckBox("enableRedirectAuthenticationEntryPoint"));

        add(new HelpLink("connectionParametersHelp", this).setDialog(dialog));

        add(new HelpLink("postLogoutRedirectUriHelp", this).setDialog(dialog));
        add(new TextField<>("postLogoutRedirectUri"));
    }

    private void addProviderComponents(RepeatingView pView, String pProviderKey, String pProviderLabel) {

        WebMarkupContainer lContainer = new WebMarkupContainer(pView.newChildId());
        pView.add(lContainer);

        lContainer.add(createLabelResourceWithParams("providerHeadline", pProviderLabel));

        WebMarkupContainer lSHContainer = new WebMarkupContainer("settings");
        lSHContainer.setOutputMarkupId(true);

        lContainer.add(lSHContainer);

        IModel<Boolean> lModel = new PropertyModel<>(configModel.getObject(), pProviderKey + "Enabled");
        CheckBox cb = new CheckBox("enabled", lModel);
        lContainer.add(cb);
        cb.add(new ToggleDisplayCheckboxBehavior(lSHContainer));

        lSHContainer.add(createLabelResourceWithParams("infoFromProvider", pProviderLabel));
        lSHContainer.add(createLabelResourceWithParams("infoForProvider", pProviderLabel));
        lSHContainer.add(new HelpLink("connectionFromParametersHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("clientId", pProviderKey));
        lSHContainer.add(new HelpLink("clientIdHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("clientSecret", pProviderKey));
        lSHContainer.add(new HelpLink("clientSecretHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("userNameAttribute", pProviderKey));
        lSHContainer.add(new HelpLink("userNameAttributeHelp", this).setDialog(dialog));

        TextField<String> lRedirectUriField = createTextField("redirectUri", pProviderKey, false);
        lRedirectUriField.setOutputMarkupId(true);
        redirectUriComponents.add(lRedirectUriField);
        lSHContainer.add(lRedirectUriField);

        lSHContainer.add(new HelpLink("connectionForParametersHelp", this).setDialog(dialog));
        lSHContainer.add(new HelpLink("redirectUriHelp", this).setDialog(dialog));

        // -- Provider specifics below --

        boolean lSupportsScope = pProviderKey.equals(PREFIX_MS) || pProviderKey.equals(PREFIX_OIDC);
        WebMarkupContainer lScopeContainer = new WebMarkupContainer("displayOnScopeSupport");
        lSHContainer.add(lScopeContainer);
        if (lSupportsScope) {
            lScopeContainer.add(createTextField("scopes", pProviderKey));
            lScopeContainer.add(new HelpLink("scopesHelp", this).setDialog(dialog));
        } else {
            lScopeContainer.setVisible(false);
        }

        boolean lOidc = pProviderKey.equals(PREFIX_OIDC);
        WebMarkupContainer lOidcContainer = new WebMarkupContainer("displayOnOidc");
        lSHContainer.add(lOidcContainer);
        if (lOidc) {
            lOidcContainer.add(new DiscoveryPanel("topPanel"));
            lOidcContainer.add(new HelpLink("oidcTokenUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new HelpLink("oidcAuthorizationUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new HelpLink("oidcUserInfoUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcForceAuthorizationUriHttps"));
            lOidcContainer.add(new CheckBox("oidcForceTokenUriHttps"));
            lOidcContainer.add(new TextField<>("oidcTokenUri"));
            lOidcContainer.add(new TextField<>("oidcAuthorizationUri"));
            lOidcContainer.add(new TextField<>("oidcUserInfoUri"));
            lOidcContainer.add(new HelpLink("oidcJwkSetUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new TextField<>("oidcJwkSetUri"));
            lOidcContainer.add(new HelpLink("oidcResponseModeHelp", this).setDialog(dialog));
            lOidcContainer.add(new TextField<>("oidcResponseMode"));
            lOidcContainer.add(new HelpLink("oidcEnforceTokenValidationHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcEnforceTokenValidation"));

            lOidcContainer.add(new HelpLink("oidcAuthenticationMethodPostSecretHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcAuthenticationMethodPostSecret"));

            lOidcContainer.add(new HelpLink("oidcUsePKCEHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcUsePKCE"));

            lOidcContainer.add(new HelpLink("oidcAllowUnSecureLoggingHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcAllowUnSecureLogging"));

            lOidcContainer.add(new HelpLink("oidcLogoutUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new TextField<>("oidcLogoutUri"));

            lOidcContainer.add(new HelpLink("oidcAdvancedSettingsHelp", this).setDialog(dialog));
            lOidcContainer.add(new HelpLink("oidcProviderSettingsHelp", this).setDialog(dialog));

        } else {
            lOidcContainer.setVisible(false);
        }
    }

    /**
     * @param pKey
     * @param pParams
     * @return a {@link Label} with {@link StringResourceModel} and parameters set
     */
    private Label createLabelResourceWithParams(String pKey, Object... pParams) {
        StringResourceModel lModel = new StringResourceModel(pKey);
        lModel.setParameters(pParams);
        Label lLabel = new Label(pKey, lModel);
        return lLabel;
    }

    private TextField<String> createTextField(String pFieldName, String pProviderName) {
        return createTextField(pFieldName, pProviderName, true);
    }

    private TextField<String> createTextField(String pAttr, String pProvider, boolean pEnabled) {
        String lModelField = pProvider + StringUtils.capitalize(pAttr);
        IModel<String> lModel = new PropertyModel<>(configModel.getObject(), lModelField);
        TextField<String> lTextField = new TextField<>(pAttr, lModel);
        lTextField.setEnabled(pEnabled);
        return lTextField;
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

    public IModel<GeoServerOAuth2LoginFilterConfig> getConfigModel() {
        return this.configModel;
    }
}
