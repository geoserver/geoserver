/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.util.NamedImpl;

/**
 * A database configuration used by tasks.
 *
 * @author Niels Charlier
 */
public class ExternalGSImpl extends NamedImpl implements ExternalGS {

    private String url;

    private String username;

    private String password;

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
