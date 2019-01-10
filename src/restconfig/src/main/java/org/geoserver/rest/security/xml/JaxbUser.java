/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import java.io.IOException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;

@XmlRootElement(name = "user")
public class JaxbUser {

    protected String userName;
    protected String password;
    protected Boolean enabled;

    public JaxbUser() {}

    public JaxbUser(GeoServerUser user) {
        this.userName = user.getUsername();
        this.enabled = Boolean.valueOf(user.isEnabled());
    }

    @XmlElement
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @XmlElement
    public String getPassword() {
        return password;
    }

    public void setPassword(String passwd) {
        this.password = passwd;
    }

    @XmlElement
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public GeoServerUser toUser(GeoServerUserGroupService service) throws IOException {
        return service.createUserObject(userName, password, enabled);
    }

    public GeoServerUser toUser(GeoServerUser user) {
        if (password != null) {
            user.setPassword(password);
        }
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        return user;
    }
}
