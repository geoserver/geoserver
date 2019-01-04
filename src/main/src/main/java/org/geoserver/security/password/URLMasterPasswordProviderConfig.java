/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.net.URL;

/**
 * Config object for {@link URLMasterPasswordProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class URLMasterPasswordProviderConfig extends MasterPasswordProviderConfig {

    /** default password filename */
    public static final String MASTER_PASSWD_FILENAME = "passwd";

    URL url;
    boolean encrypting;

    public URLMasterPasswordProviderConfig() {}

    public URLMasterPasswordProviderConfig(URLMasterPasswordProviderConfig other) {
        super(other);

        this.url = other.getURL();
    }

    /** The url providing the source (and optionally store) of the password. */
    public URL getURL() {
        return url;
    }

    /** Sets the url providing the source (and optionally store) of the password. */
    public void setURL(URL url) {
        this.url = url;
    }

    /** Flag controlling whether passwords are stored encrypted. */
    public boolean isEncrypting() {
        return encrypting;
    }

    /** Sets flag controlling whether passwords are stored encrypted. */
    public void setEncrypting(boolean encrypting) {
        this.encrypting = encrypting;
    }
}
