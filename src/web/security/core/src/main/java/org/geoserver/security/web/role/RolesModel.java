/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerApplication;

public class RolesModel extends LoadableDetachableModel<Collection<GeoServerRole>> {

    @Override
    protected Collection<GeoServerRole> load() {
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        try {
            return new ArrayList(secMgr.getActiveRoleService().getRoles());
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }
    }
}
