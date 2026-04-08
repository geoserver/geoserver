/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.resourceserver;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.oauth2.resourceserver.GeoServerOAuth2ResourceServerFilterConfig;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2ResourceServerAuthProviderPanel
        extends PreAuthenticatedUserNameFilterPanel<GeoServerOAuth2ResourceServerFilterConfig> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -3025321797363970333L;

    private GeoServerDialog dialog;

    public OAuth2ResourceServerAuthProviderPanel(String id, IModel<GeoServerOAuth2ResourceServerFilterConfig> model) {
        super(id, model);

        this.dialog = (GeoServerDialog) get("dialog");

        add(new HelpLink("resourceServerParametersHelp", this).setDialog(dialog));

        add(new TextField<>("issuerUri"));
        add(new HelpLink("issuerUriHelp", this).setDialog(dialog));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        return super.getRoleSourcePanel(model);
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources = new ArrayList<>(Arrays.asList(OpenIdRoleSource.values()));
        sources.addAll(Arrays.asList(PreAuthenticatedUserNameRoleSource.values()));
        return new DropDownChoice<>("roleSource", sources, new RoleSourceChoiceRenderer());
    }
}
