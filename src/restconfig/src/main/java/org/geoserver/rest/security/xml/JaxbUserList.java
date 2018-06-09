/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.security.impl.GeoServerUser;

@XmlRootElement(name = "users")
public class JaxbUserList {

    protected List<JaxbUser> users;

    public JaxbUserList() {}

    public JaxbUserList(Collection<GeoServerUser> users) {
        this.users = new ArrayList<JaxbUser>();
        for (GeoServerUser user : users) {
            this.users.add(new JaxbUser(user));
        }
    }

    @XmlElement(name = "user")
    public List<JaxbUser> getUsers() {
        return users;
    }
}
