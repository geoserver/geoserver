/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

/** CogSettings specific for a store, including therefore eventual authentication info */
public class CogSettingsStore extends CogSettings {

    public CogSettingsStore(CogSettings settings) {
        super(settings);
    }

    public CogSettingsStore() {
        super();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    String username;

    String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
