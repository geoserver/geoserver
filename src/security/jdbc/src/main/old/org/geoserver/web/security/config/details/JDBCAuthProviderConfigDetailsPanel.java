/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.config.details;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.web.security.config.SecurityNamedConfigModelHelper;

/**
 * A form component that can be used for xml configurations
 */
public class JDBCAuthProviderConfigDetailsPanel extends AbstractAuthenticationProviderDetailsPanel{
    private static final long serialVersionUID = 1L;
    TextField<String> driverNameComponent;
    TextField<String> connectURLComponent;
    
    public JDBCAuthProviderConfigDetailsPanel(String id, CompoundPropertyModel<SecurityNamedConfigModelHelper> model) {
        super(id,model);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        driverNameComponent = new TextField<String>("config.driverClassName");
        add(driverNameComponent);
        connectURLComponent = new TextField<String>("config.connectURL");
        add(connectURLComponent);
    };
        
    
    @Override
    protected SecurityNamedServiceConfig createNewConfigObject() {
        return new JDBCConnectAuthProviderConfig();
    }
 
    @Override
    public void updateModel() {
        super.updateModel();
        driverNameComponent.updateModel();
        connectURLComponent.updateModel();
    }
}
