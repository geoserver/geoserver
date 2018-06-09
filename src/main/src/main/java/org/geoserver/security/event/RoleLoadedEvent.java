/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.event;

import java.util.EventObject;
import org.geoserver.security.GeoServerRoleService;

/**
 * Event fired after loading roles from the backend store into memory
 *
 * <p>This event is intended for stateful services of type {@link GeoServerRoleService}. If the
 * backend is changed externally and a reload occurs, listeners should be notified.
 *
 * @author christian
 */
public class RoleLoadedEvent extends EventObject {

    /** */
    private static final long serialVersionUID = 1L;

    public RoleLoadedEvent(GeoServerRoleService source) {
        super(source);
    }

    public GeoServerRoleService getService() {
        return (GeoServerRoleService) getSource();
    }
}
