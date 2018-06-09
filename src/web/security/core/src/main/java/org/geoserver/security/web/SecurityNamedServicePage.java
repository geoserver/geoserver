/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.logging.Logging;

/**
 * Base page for SecurityNamedServiceConfig new and edit pages.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SecurityNamedServicePage<T extends SecurityNamedServiceConfig>
        extends AbstractSecurityPage {

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.web.security");

    /** current config panel */
    protected SecurityNamedServicePanel<T> panel;

    public SecurityNamedServicePage() {}

    protected StringResourceModel createTitleModel(Class serviceClass) {
        return new StringResourceModel(serviceClass.getName() + ".title", new Model());
    }

    protected StringResourceModel createTitleModel(SecurityNamedServicePanelInfo panelInfo) {
        return new StringResourceModel(panelInfo.getTitleKey(), new Model());
    }

    protected StringResourceModel createDescriptionModel(SecurityNamedServicePanelInfo panelInfo) {
        return new StringResourceModel(panelInfo.getDescriptionKey(), new Model());
    }

    protected StringResourceModel createShortTitleModel(SecurityNamedServicePanelInfo panelInfo) {
        return new StringResourceModel(panelInfo.getShortTitleKey(), new Model());
    }

    protected SecurityNamedServicePanel<T> createPanel(
            String id, SecurityNamedServicePanelInfo panelInfo, IModel<T> config) {
        try {
            SecurityNamedServicePanel panel =
                    (SecurityNamedServicePanel<T>)
                            panelInfo
                                    .getComponentClass()
                                    .getConstructor(String.class, IModel.class)
                                    .newInstance(id, config);
            return panel;
        } catch (Exception e) {
            throw new WicketRuntimeException(e);
        }
    }

    protected void handleSubmit(Form<?> form) {
        T config = (T) form.getModelObject();
        try {
            panel.doSave(config);
            doReturn();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving config", e);
            error(e);
        }
    }
}
