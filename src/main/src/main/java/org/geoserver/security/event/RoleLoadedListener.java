/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.event;

import java.util.EventListener;

/**
 * Classes implementing this interfaces may register 
 * for notifications on a successful load from backend 
 * 
 * @author christian
 *
 */
public interface RoleLoadedListener extends EventListener {
    public void rolesChanged(RoleLoadedEvent event);
}
