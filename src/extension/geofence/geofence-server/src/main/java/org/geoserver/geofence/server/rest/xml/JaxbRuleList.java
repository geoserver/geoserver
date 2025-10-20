/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.geofence.core.model.Rule;

@XmlRootElement(name = "Rules")
public class JaxbRuleList {

    protected List<JaxbRule> list;

    protected long count;

    public JaxbRuleList() {}

    public JaxbRuleList(long count) {
        this.count = count;
    }

    public JaxbRuleList(List<Rule> list) {
        this.list = new ArrayList<>();
        for (Rule rule : list) {
            this.list.add(new JaxbRule(rule));
        }
        this.count = list.size();
    }

    @XmlAttribute
    public long getCount() {
        return count;
    }

    @XmlElement(name = "Rule")
    public List<JaxbRule> getRules() {
        return list;
    }
}
