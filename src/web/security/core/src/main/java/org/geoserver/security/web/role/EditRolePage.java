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
 * Page for editing a {@link GeoServerRole} object
 *
 * @author christian
 */
public class EditRolePage extends AbstractRolePage {

    public EditRolePage(String roleServiceName, GeoServerRole role) {
        // parent role name not known at this moment, parent
        // constructor will do the job
        super(roleServiceName, role);

        get("form:name").setEnabled(false);

        // do we have a personalized role?
        if (role.getUserName() != null) {
            get("form:properties").setEnabled(false);
            get("form:parent").setEnabled(false);
            get("form:save").setEnabled(false);
        }
    }

    @Override
    protected void onFormSubmit(GeoServerRole updated) throws IOException {

        if (hasRoleStore(roleServiceName) == false) {
            throw new RuntimeException(
                    "Invalid workflow, cannot store in a read only role service");
        }

        GeoServerRoleStore store = null;
        try {
            store = new RoleStoreValidationWrapper(getRoleStore(roleServiceName));

            GeoServerRole role = store.getRoleByName(updated.getAuthority());

            role.getProperties().clear();
            role.getProperties().putAll(updated.getProperties());
            store.updateRole(role);

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
