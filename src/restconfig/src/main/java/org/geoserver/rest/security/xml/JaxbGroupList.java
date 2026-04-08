/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geoserver.security.impl.GeoServerUserGroup;

@XmlRootElement(name = "groups")
public class JaxbGroupList {

    protected List<String> groupNames;

    public JaxbGroupList() {}

    public JaxbGroupList(Collection<GeoServerUserGroup> groups) {
        groupNames = new ArrayList<>();
        for (GeoServerUserGroup group : groups) {
            groupNames.add(group.getGroupname());
        }
    }

    @XmlElement(name = "group")
    public List<String> getGroups() {
        return groupNames;
    }
}
