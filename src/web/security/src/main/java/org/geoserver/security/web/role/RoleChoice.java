/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerApplication;

/**
 * Choice widget for roles from a specific role service configuration.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RoleChoice extends DropDownChoice<String> {

    public RoleChoice(String id, IModel<? extends SecurityRoleServiceConfig> configModel) {
        super(id, new RoleChoiceNameModel(configModel), new RoleChoiceRenderer());
    }

    static class RoleChoiceNameModel implements IModel<List<String>> {

        IModel<? extends SecurityRoleServiceConfig> configModel;

        RoleChoiceNameModel(IModel<? extends SecurityRoleServiceConfig> configModel) {
            this.configModel = configModel;
        }

        @Override
        public List<String> getObject() {
            SecurityRoleServiceConfig config = configModel.getObject();
            List<String> roleNames = new ArrayList<String>();
            if (config.getId() != null) {
                try {
                    for (GeoServerRole role : GeoServerApplication.get().getSecurityManager()
                            .loadRoleService(config.getName()).getRoles()) {
                        roleNames.add(role.getAuthority());
                    }
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }
            }
            return roleNames;
        }

        @Override
        public void detach() {
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }
    }

    static class RoleChoiceRenderer extends ChoiceRenderer<String> {
        @Override
        public Object getDisplayValue(String object) {
            return object;
        }
        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }
}
