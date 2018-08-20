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

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.web.wicket.HelpLink;

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
    }
}
