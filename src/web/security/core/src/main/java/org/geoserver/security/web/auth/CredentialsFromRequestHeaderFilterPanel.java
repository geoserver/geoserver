/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.CredentialsFromRequestHeaderFilterConfig;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerCredentialsFromRequestHeaderAuthenticationFilter}.
 *
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class CredentialsFromRequestHeaderFilterPanel
        extends AuthenticationFilterPanel<CredentialsFromRequestHeaderFilterConfig> {
    private static final long serialVersionUID = 1;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    GeoServerDialog dialog;

    IModel<CredentialsFromRequestHeaderFilterConfig> model;

    public CredentialsFromRequestHeaderFilterPanel(
            String id, IModel<CredentialsFromRequestHeaderFilterConfig> model) {
        super(id, model);

        dialog = (GeoServerDialog) get("dialog");
        this.model = model;

        add(new HelpLink("authHeaderParametersHelp", this).setDialog(dialog));

        add(new TextField<String>("userNameHeaderName"));
        add(new TextField<String>("passwordHeaderName"));
        add(new TextField<String>("userNameRegex"));
        add(new TextField<String>("passwordRegex"));
        add(new CheckBox("parseAsUriComponents"));
    }
}
