/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.rest;


import java.util.Map;
import java.util.Map.Entry;

import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;

/**
 * REST Resource for reading and changing service access rules
 * 
 * @author christian
 *
 */
public class ServiceAccessControlResource extends AbstractAccessControlResource {

    public ServiceAccessControlResource() {
        super(ServiceAccessRuleDAO.get());
    }

    @Override
    protected void addRuleToMap(Comparable rule, Map map) {
        ServiceAccessRule ruleObject = (ServiceAccessRule) rule;
        map.put(ruleObject.getKey(), ruleObject.getValue());
    }

    @Override
    protected String keyFor(Comparable rule) {
        return ((ServiceAccessRule) rule).getKey();
    }

    @Override
    protected Comparable convertEntryToRule(Entry entry) {
        String[] parts = parseElements((String) entry.getKey());
        return new ServiceAccessRule(parts[0], parts[1], parseRoles((String) entry.getValue()));
    }

    @Override
    protected String validateRuleKey(String ruleKey) {
        String[] elements = parseElements(ruleKey);
        if (elements.length != 2) {
            return "Invalid rule " + ruleKey
                    + ", the expected format is service.method=role1,role2,...";
        }

        if (ANY.equals(elements[0])) {
            if (!ANY.equals(elements[1])) {
                return "Invalid rule " + ruleKey + ", when namespace "
                        + "is * then also layer must be *.";
            }
        }

        return null;
    }

    private String[] parseElements(String path) {
        // regexp: ignore extra spaces, split on dot
        return path.split("\\s*\\.\\s*");
    }

}
