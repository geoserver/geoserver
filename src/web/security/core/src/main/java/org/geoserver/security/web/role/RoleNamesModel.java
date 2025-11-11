/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerApplication;

/** A model that wraps a collection of role names (strings) and exposes them as a list of GeoServerRole objects. */
public class RoleNamesModel extends LoadableDetachableModel<List<GeoServerRole>> {

    IModel<Collection<String>> roleNamesModel;

    public RoleNamesModel(IModel<Collection<String>> roleNamesModel) {
        this.roleNamesModel = roleNamesModel;
    }

    @Override
    protected List<GeoServerRole> load() {

        Map<String, GeoServerRole> roleMap = new HashMap<>();
        try {
            for (GeoServerRole role :
                    GeoServerApplication.get().getSecurityManager().getRolesForAccessControl())
                roleMap.put(role.getAuthority(), role);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<GeoServerRole> roles = new ArrayList<>();
        for (String roleName : roleNamesModel.getObject()) {
            GeoServerRole role = roleMap.get(roleName);
            if (role != null) roles.add(role);
        }
        return roles;
    }

    @Override
    public void setObject(List<GeoServerRole> object) {
        super.setObject(object);

        // set back to the delegate model
        Collection<String> roleNames = roleNamesModel.getObject();
        roleNames.clear();

        for (GeoServerRole role : object) {
            roleNames.add(role.getAuthority());
        }
        roleNamesModel.setObject(roleNames);
    }
}
