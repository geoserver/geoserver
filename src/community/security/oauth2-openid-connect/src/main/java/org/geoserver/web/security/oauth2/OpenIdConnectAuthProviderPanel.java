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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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

    public OpenIdConnectAuthProviderPanel(String id, IModel<OpenIdConnectFilterConfig> model) {
        super(id, model);

        add(new HelpLink("principalKeyHelp", this).setDialog(dialog));
        add(new TextField<String>("principalKey"));

        add(new HelpLink("jwkURIHelp", this).setDialog(dialog));
        add(new TextField<String>("jwkURI"));
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (IdToken.equals(model) || AccessToken.equals(model)) {
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
