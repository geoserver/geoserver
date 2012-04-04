/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;

import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.validation.RoleStoreValidationWrapper;

/**
 * Page for adding a new {@link GeoServerRole} object
 * 
 * @author christian
 *
 */
public class NewRolePage extends AbstractRolePage {

    public NewRolePage(String roleServiceName) {
        super(roleServiceName, new GeoServerRole(GeoServerRole.NULL_ROLE.getAuthority()));
        
        if (hasRoleStore(roleServiceName)==false) {
            throw new RuntimeException("Workflow error, new role not possible for read only service");
        }
    }

    @Override
    protected void onFormSubmit(GeoServerRole role) throws IOException {
        
        GeoServerRoleStore store = null;
        try {

            //copy into a new one so we can set the name properly
            GeoServerRole newRole = 
                new GeoServerRole(get("form:name").getDefaultModelObjectAsString());
            newRole.setUserName(role.getUserName());
            newRole.getProperties().putAll(role.getProperties());
            role = newRole;

            store = new RoleStoreValidationWrapper(getRoleStore(roleServiceName));
            store.addRole(role);

            String parentRoleName = get("form:parent").getDefaultModelObjectAsString();
            if (parentRoleName != null) {
                GeoServerRole parentRole = store.getRoleByName(parentRoleName);
                store.setParentRole(role, parentRole);
            }

            store.store();
        } catch (IOException ex) {
            try {store.load(); } catch (IOException ex2) {};
            throw ex;
        }
    }

}
