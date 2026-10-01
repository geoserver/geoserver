/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

/**
 * Base class for configuration panels of a specific class of named security service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class SecurityNamedServicePanel<T extends SecurityNamedServiceConfig> extends FormComponentPanel<T> {

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.web.security");

    /** model for underlying config */
    protected IModel<T> configModel;

    /** pop-up dialog */
    protected GeoServerDialog dialog;

    public SecurityNamedServicePanel(String id, IModel<T> model) {
        super(id, new Model<>());
        this.configModel = model;

        // check for administrator, if not disable the panel and emit warning message
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole();
        setEnabled(isAdmin);

        add(new Label("message", isAdmin ? new Model<>() : new StringResourceModel("notAdmin", this, null)));
        if (!isAdmin) {
            get("message").add(new AttributeAppender("class", new Model<>("info-link"), " "));
        }

        setOutputMarkupId(true);
        add(new TextField("name").setRequired(true).setEnabled(model.getObject().getId() == null));

        add(dialog = new GeoServerDialog("dialog"));
    }

    protected GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    /** Determines if the configuration object represents a new configuration, or an existing one. */
    protected boolean isNew() {
        return configModel.getObject().getId() == null;
    }

    public abstract void doSave(T config) throws Exception;

    public abstract void doLoad(T config) throws Exception;

    /**
     * Shows the error of a failed connection test on the component, unless a field error, such as a missing required
     * value, already explains the failure.
     */
    public static void reportConnectionError(Exception e, Component component) {
        if (new FeedbackCollector(component).collect(FeedbackMessage::isError).isEmpty()) {
            component.error(errorMessage(e, component));
        }
    }

    /**
     * Returns the message to show for an error raised while saving or testing a service. Security exceptions are looked
     * up as {@code <ExceptionClass>.<id>} in the GeoServerApplication properties, so they follow the user interface
     * language. Without a matching key the exception message is used, for other exceptions the exception class and
     * message.
     */
    public static String errorMessage(Exception e, Component component) {
        if (e instanceof AbstractSecurityException) {
            AbstractSecurityException se = (AbstractSecurityException) e;
            String key = se.getClass().getSimpleName() + "." + se.getId();
            // a default value would go through MessageFormat as well, so the fallback is returned as is
            if (component.getLocalizer().getStringIgnoreSettings(key, component, null, null) != null) {
                return new ParamResourceModel(key, component, se.getArgs()).getString();
            }
            return se.getMessage();
        }
        return e.toString();
    }
}
