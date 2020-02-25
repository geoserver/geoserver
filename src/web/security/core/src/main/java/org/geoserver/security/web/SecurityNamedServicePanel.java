/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.logging.Logger;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.util.logging.Logging;

/**
 * Base class for configuration panels of a specific class of named security service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class SecurityNamedServicePanel<T extends SecurityNamedServiceConfig>
        extends FormComponentPanel {

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.web.security");

    /** model for underlying config */
    protected IModel<T> configModel;

    /** pop-up dialog */
    protected GeoServerDialog dialog;

    public SecurityNamedServicePanel(String id, IModel<T> model) {
        super(id, new Model());
        this.configModel = model;

        // check for administrator, if not disable the panel and emit warning message
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole();
        setEnabled(isAdmin);

        add(
                new Label(
                        "message",
                        isAdmin ? new Model() : new StringResourceModel("notAdmin", this, null)));
        if (!isAdmin) {
            get("message").add(new AttributeAppender("class", new Model("info-link"), " "));
        }

        setOutputMarkupId(true);
        add(new TextField("name").setRequired(true).setEnabled(model.getObject().getId() == null));

        add(dialog = new GeoServerDialog("dialog"));
    }

    protected GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    /**
     * Determines if the configuration object represents a new configuration, or an existing one.
     */
    protected boolean isNew() {
        return configModel.getObject().getId() == null;
    }

    public abstract void doSave(T config) throws Exception;

    public abstract void doLoad(T config) throws Exception;
}
