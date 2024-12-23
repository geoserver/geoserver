/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.web;

import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JSON;
import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptContentHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.wicket.HelpLink;

/** Jwt Headers auth panel Wicket */
public class JwtHeadersAuthFilterPanel extends PreAuthenticatedUserNameFilterPanel<GeoServerJwtHeadersFilterConfig> {

    static String jwtHeadersAuthFilterPanelCSS;
    static String jwtHeadersAuthFilterPanelJS;

    static {
        try {
            jwtHeadersAuthFilterPanelCSS = CharStreams.toString(new InputStreamReader(
                    JwtHeadersAuthFilterPanel.class.getResourceAsStream(
                            "/org/geoserver/security/jwtheaders/web/css/jwt-headers-auth-filter-panel.css"),
                    Charsets.UTF_8));
            jwtHeadersAuthFilterPanelJS = CharStreams.toString(new InputStreamReader(
                    JwtHeadersAuthFilterPanel.class.getResourceAsStream(
                            "/org/geoserver/security/jwtheaders/web/js/jwt-headers-auth-filter-panel.js"),
                    Charsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected DropDownChoice<JwtConfiguration.UserNameHeaderFormat> userNameFormatChoice;

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

        userNameFormatChoice = new DropDownChoice(
                "userNameFormatChoice",
                Arrays.asList(JwtConfiguration.UserNameHeaderFormat.values()),
                new UserNameFormatChoiceRenderer());

        add(userNameFormatChoice);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // Content-Security-Policy: inline styles must be nonce=...
        String css = "  ul.horizontal div {\n" + "            display: inline;\n" + "        }";
        response.render(CssHeaderItem.forCSS(css, "org-geoserver-security-web-data-JwtHeadersAuthFilterPanel"));

        // add css
        response.render(CssHeaderItem.forCSS(jwtHeadersAuthFilterPanelCSS, "jwtHeadersAuthFilterPanelCSS"));

        // add js script
        response.render(
                JavaScriptContentHeaderItem.forScript(jwtHeadersAuthFilterPanelJS, "jwtHeadersAuthFilterPanelJS"));

        // setup onchange handlers
        // setup onClick events (Content-security-policy doesn't allow onClick events in the HTML)
        String script = "\n";

        script += "$('#userNameFormatSelect').on('change',function() { usernameFormatChanged(); } \n);\n\n";
        script += "$('#validateToken').on('change',function() { showTokenValidationChanged(); } \n);\n\n";

        script +=
                "$('#validateTokenSignature').on('change',function() { toggleVisible(this,'validateTokenSignatureURLDiv'); } \n);\n\n";
        script +=
                "$('#validateTokenAgainstURL').on('change',function() { toggleVisible(this,'validateTokenAgainstURLDiv'); } \n);\n\n";
        script +=
                "$('#validateTokenAudience').on('change',function() { toggleVisible(this,'validateTokenAudienceDiv'); } \n);\n\n";

        // make sure reset() is called
        // this one is called during an edit
        script += "reset();";
        // this one is called during a "new" jwt-header
        script += "setTimeout(reset,100);";

        script += "\n";
        response.render(OnDomReadyHeaderItem.forScript(script));
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources =
                new ArrayList<>(Arrays.asList(GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.values()));
        sources.addAll(Arrays.asList(PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.values()));
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

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            // when wicket loads the page, we want this to fire AFTER the
            // elements are in the dom.  This is so the converter is run
            // when the page first loads (and table is updated).
            String script = " roleConverterStringChanged();\n";
            script += "$('#roleConverterString').on('input',function() { roleConverterStringChanged(this); } \n);\n\n";

            response.render(OnDomReadyHeaderItem.forScript(script));

            // Content-Security-Policy: inline styles must be nonce=...
            String css = "  #roleConverterStringTable table, th, td {\n"
                    + "            border: black 1px solid !important;\n"
                    + "        }";
            response.render(CssHeaderItem.forCSS(
                    css, "org-geoserver-security-web-data-JwtHeaderAuthFilterPanel-JsonClaimPanel"));
        }
    }
}
