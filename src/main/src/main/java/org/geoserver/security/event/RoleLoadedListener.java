/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.event;

import java.util.EventListener;

/**
 * Classes implementing this interfaces may register for notifications on a successful load from
 * backend
 *
 * @author christian
 */
public interface RoleLoadedListener extends EventListener {
    public void rolesChanged(RoleLoadedEvent event);
}
