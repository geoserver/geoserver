/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.web.SecurityNamedServicePanel;
import org.geoserver.web.wicket.HelpLink;

/**
 * Base class for master password provider panels.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProviderPanel<T extends MasterPasswordProviderConfig>
        extends SecurityNamedServicePanel<T> {

    public MasterPasswordProviderPanel(String id, IModel<T> model) {
        super(id, model);

        add(new CheckBox("readOnly"));
        add(new CheckBox("loginEnabled"));
        add(new HelpLink("settingsHelp", this).setDialog(dialog));
    }

    @Override
    public void doSave(T config) throws Exception {
        getSecurityManager().saveMasterPasswordProviderConfig(config);
    }

    @Override
    public void doLoad(T config) throws Exception {
        getSecurityManager().loadMasterPassswordProviderConfig(config.getName());
    }
}
