/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.web;

import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JSON;
import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.wicket.HelpLink;

/** Jwt Headers auth panel Wicket */
public class JwtHeadersAuthFilterPanel
        extends PreAuthenticatedUserNameFilterPanel<GeoServerJwtHeadersFilterConfig> {

    protected DropDownChoice<GeoServerJwtHeadersFilterConfig.UserNameHeaderFormat>
            userNameFormatChoice;

    public JwtHeadersAuthFilterPanel(String id, IModel<GeoServerJwtHeadersFilterConfig> model) {
        super(id, model);

        add(new HelpLink("UsernameHelp", this).setDialog(dialog));
        add(new TextField("userNameHeaderAttributeName").setRequired(true));

        add(new TextField("userNameJsonPath").setRequired(false));

        add(new CheckBox("validateToken").setRequired(false));
        add(new HelpLink("validateTokenHelp", this).setDialog(dialog));

        add(new CheckBox("validateTokenExpiry").setRequired(false));

        add(new CheckBox("validateTokenSignature").setRequired(false));
        add(new TextField("validateTokenSignatureURL").setRequired(false));

        add(new CheckBox("validateTokenAgainstURL").setRequired(false));
        add(new TextField("validateTokenAgainstURLEndpoint").setRequired(false));
        add(new CheckBox("validateSubjectWithEndpoint").setRequired(false));

        add(new CheckBox("validateTokenAudience").setRequired(false));
        add(new TextField("validateTokenAudienceClaimName").setRequired(false));
        add(new TextField("validateTokenAudienceClaimValue").setRequired(false));

        userNameFormatChoice =
                new DropDownChoice(
                        "userNameFormatChoice",
                        Arrays.asList(
                                GeoServerJwtHeadersFilterConfig.UserNameHeaderFormat.values()),
                        new UserNameFormatChoiceRenderer());

        add(userNameFormatChoice);
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources =
                new ArrayList<>(
                        Arrays.asList(
                                GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.values()));
        sources.addAll(
                Arrays.asList(
                        PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                                .values()));
        return new DropDownChoice<>("roleSource", sources, new RoleSourceChoiceRenderer());
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (JSON.equals(model) || JWT.equals(model)) {
            return new JsonClaimPanel("panel");
        }
        return super.getRoleSourcePanel(model);
    }

    static class JsonClaimPanel extends Panel {
        public JsonClaimPanel(String id) {
            super(id, new Model<>());
            add(new TextField<String>("rolesJsonPath").setRequired(true));
            add(new TextField<String>("rolesHeaderName").setRequired(true));
            add(new TextField<String>("roleConverterString").setRequired(false));
            add(new CheckBox("onlyExternalListedRoles").setRequired(false));
        }
    }
}
