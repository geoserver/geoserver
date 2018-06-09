/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.util.IProvider;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.crypt.NoCrypt;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Returns an ICrypt that actually encrypts the urls, or not, depending on the security manager
 * settings
 *
 * @author Andrea Aime - GeoSolutions
 */
class GeoServerCryptProvider implements IProvider<ICrypt> {

    GeoServerSecurityManager manager;
    volatile ICrypt theCrypt;

    public GeoServerCryptProvider(GeoServerSecurityManager manager) {
        this.manager = manager;
    }

    @Override
    public ICrypt get() {
        // fully dynamic lookup as I'm not sure if this method gets called
        // before or after the application gets its app contxt
        if (manager.isEncryptingUrlParams()) {
            return getCrypt();
        } else {
            return new NoCrypt();
        }
    }

    private ICrypt getCrypt() {
        // lazy init via double checked locking (with volatile, should be safe)
        // because we cannot get to the settings untile after the whole app startup is done
        if (theCrypt == null) {
            synchronized (this) {
                if (theCrypt == null) {
                    GeoServerApplication application = GeoServerApplication.get();
                    theCrypt = application.getSecuritySettings().getCryptFactory().newCrypt();
                }
            }
        }
        return theCrypt;
    }
}
