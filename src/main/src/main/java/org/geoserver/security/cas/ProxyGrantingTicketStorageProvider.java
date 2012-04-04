/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.cas;

import org.geoserver.platform.GeoServerExtensions;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorageImpl;

/**
 * provides a global {@link ProxyGrantingTicketStorage} object 
 * 
 * @author christian
 *
 */
public class ProxyGrantingTicketStorageProvider {
    
    static protected ProxyGrantingTicketStorage Singleton;
    
    static ProxyGrantingTicketStorage get() {
        
        if (Singleton !=null) return Singleton;
        
        Singleton=GeoServerExtensions.bean(ProxyGrantingTicketStorage.class);
        if (Singleton==null)
            Singleton = new ProxyGrantingTicketStorageImpl();
        
        return Singleton;
    }

}
