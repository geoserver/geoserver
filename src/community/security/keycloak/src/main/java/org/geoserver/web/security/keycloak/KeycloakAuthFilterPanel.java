/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.geoserver.security.keycloak.GeoServerKeycloakFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * UI for configuring Keycloak auth filters. contains just a single text-field, since the Keycloak
 * server will generally provide correctly-formatted JSON to copy-paste into this box. No cause to
 * over-complicate the UI.
 */
public class KeycloakAuthFilterPanel
        extends AuthenticationFilterPanel<GeoServerKeycloakFilterConfig> {

    private static final Logger LOG = Logging.getLogger(KeycloakAuthFilterPanel.class);

    private static final long serialVersionUID = 1L;

    public KeycloakAuthFilterPanel(String id, IModel<GeoServerKeycloakFilterConfig> model) {
        super(id, model);
        LOG.log(Level.FINER, "KeycloakAuthFilterPanel.[constructor] ENTRY");
        add(new HelpLink("adapterConfigHelp", this).setDialog(this.dialog));
        add(new TextArea<String>("adapterConfig"));
    }

    @Override
    public void doLoad(GeoServerKeycloakFilterConfig config) throws Exception {
        LOG.log(Level.FINER, "KeycloakAuthFilterPanel.doLoad ENTRY");
        getSecurityManager().loadFilter(config.getName());
    }

    @Override
    public void doSave(GeoServerKeycloakFilterConfig config) throws Exception {
        LOG.log(Level.FINER, "KeycloakAuthFilterPanel.doSave ENTRY");
        getSecurityManager().saveFilter(config);
    }
}
