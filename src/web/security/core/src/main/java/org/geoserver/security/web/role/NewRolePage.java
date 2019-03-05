/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
 */
public class NewRolePage extends AbstractRolePage {

    public NewRolePage(String roleServiceName) {
        super(roleServiceName, null);

        if (hasRoleStore(roleServiceName) == false) {
            throw new RuntimeException(
                    "Workflow error, new role not possible for read only service");
        }
    }

    @Override
    protected void onFormSubmit(GeoServerRole role) throws IOException {

        GeoServerRoleStore store = null;
        try {

            store = new RoleStoreValidationWrapper(getRoleStore(roleServiceName));
            // copy into a new one so we can set the name properly
            GeoServerRole newRole =
                    store.createRoleObject(get("form:name").getDefaultModelObjectAsString());
            newRole.setUserName(role.getUserName());
            newRole.getProperties().putAll(role.getProperties());
            role = newRole;
            store.addRole(role);

            String parentRoleName = get("form:parent").getDefaultModelObjectAsString();
            if (parentRoleName != null) {
                GeoServerRole parentRole = store.getRoleByName(parentRoleName);
                store.setParentRole(role, parentRole);
            }

            store.store();
        } catch (IOException ex) {
            try {
                if (store != null) store.load();
            } catch (IOException ex2) {
            }
            throw ex;
        }
    }
}
