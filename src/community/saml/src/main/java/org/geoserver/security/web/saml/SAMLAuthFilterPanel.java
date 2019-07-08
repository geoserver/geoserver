/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.saml;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.saml.SAMLAuthenticationFilter;
import org.geoserver.security.saml.SAMLAuthenticationFilterConfig;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link SAMLAuthenticationFilter}.
 *
 * @author Xandros
 */
public class SAMLAuthFilterPanel
        extends PreAuthenticatedUserNameFilterPanel<SAMLAuthenticationFilterConfig> {

    private static final long serialVersionUID = 7353031770013140878L;

    public SAMLAuthFilterPanel(String id, IModel<SAMLAuthenticationFilterConfig> model) {
        super(id, model);
        dialog = (GeoServerDialog) get("dialog");
        add(new HelpLink("samlParametersHelp", this).setDialog(dialog));

        add(new TextField<String>("entityId"));
        add(new TextField<String>("metadataURL"));
        add(new TextArea<String>("metadata"));
        add(new CheckBox("signing"));
        add(new TextField<String>("keyStorePath"));
        add(new PasswordTextField("keyStorePassword"));
        add(new TextField<String>("keyStoreId"));
        add(new PasswordTextField("keyStoreIdPassword"));
    }
}
