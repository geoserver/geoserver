/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.geofence.core.model.AdminRule;

@XmlRootElement(name = "AdminRules")
public class JaxbAdminRuleList {
    protected List<JaxbAdminRule> list;

    protected long count;

    public JaxbAdminRuleList() {}

    public JaxbAdminRuleList(long count) {
        this.count = count;
    }

    public JaxbAdminRuleList(List<AdminRule> list) {
        this.list = new ArrayList<>();
        for (AdminRule rule : list) {
            this.list.add(new JaxbAdminRule(rule));
        }
        this.count = list.size();
    }

    @XmlAttribute
    public long getCount() {
        return count;
    }

    @XmlElement(name = "AdminRule")
    public List<JaxbAdminRule> getRules() {
        return list;
    }
}
