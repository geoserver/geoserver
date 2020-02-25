/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.event;

import java.util.EventObject;
import org.geoserver.security.GeoServerUserGroupService;

/**
 * Event fired after loading user/groups from the backend store into memory
 *
 * <p>This event is intended for stateful services of type {@link GeoServerUserGroupService}. If the
 * backend is changed externally and a reload occurs, listeners should be notified.
 *
 * @author christian
 */
public class UserGroupLoadedEvent extends EventObject {

    /** */
    private static final long serialVersionUID = 1L;

    public UserGroupLoadedEvent(GeoServerUserGroupService source) {
        super(source);
    }

    public GeoServerUserGroupService getService() {
        return (GeoServerUserGroupService) getSource();
    }
}
