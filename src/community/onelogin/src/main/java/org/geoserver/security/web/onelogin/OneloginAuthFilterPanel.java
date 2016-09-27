/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.onelogin;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.onelogin.OneloginAuthenticationFilter;
import org.geoserver.security.onelogin.OneloginAuthenticationFilterConfig;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link OneloginAuthenticationFilter}.
 * 
 * @author Xandros
 */
public class OneloginAuthFilterPanel
        extends PreAuthenticatedUserNameFilterPanel<OneloginAuthenticationFilterConfig> {

    private static final long serialVersionUID = 7353031770013140878L;

    public OneloginAuthFilterPanel(String id, IModel<OneloginAuthenticationFilterConfig> model) {
        super(id, model);
        dialog = (GeoServerDialog) get("dialog");
        add(new HelpLink("oneloginParametersHelp", this).setDialog(dialog));

        add(new TextField<String>("entityId"));
        add(new TextField<String>("metadataURL"));
    }

}
