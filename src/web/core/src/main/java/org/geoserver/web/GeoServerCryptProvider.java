/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.util.IProvider;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.crypt.NoCrypt;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Returns an ICrypt that actually encrypts the urls, or not, depending on
 * the security manager settings
 * 
 * @author Andrea Aime - GeoSolutions
 */
class GeoServerCryptProvider implements IProvider<ICrypt> {

    GeoServerSecurityManager manager;
    
    public GeoServerCryptProvider(GeoServerSecurityManager manager) {
        this.manager = manager;
    }

    @Override
    public ICrypt get() {
        // fully dynamic lookup as I'm not sure if this method gets called
        // before or after the application gets its app contxt
        if (manager.isEncryptingUrlParams()) {
            GeoServerApplication application = GeoServerApplication.get();
            return application.getSecuritySettings().getCryptFactory().newCrypt();
        } else {
            return new NoCrypt();
        }
    }

}
