/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.rest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.geoserver.security.impl.RESTAccessRuleDAO;

/**
 * REST Resource for reading and changing REST access rules
 * 
 * @author christian
 *
 */
public class RESTAccessControlResource extends AbstractAccessControlResource {

    /**
     * rule pattern
     */
    static final Pattern KEYPATTERN = Pattern
            .compile("\\S+:(GET|POST|PUT|DELETE|HEAD)(,(GET|POST|PUT|DELETE|HEAD))*");

    public RESTAccessControlResource() {
        super(RESTAccessRuleDAO.get());
    }

    @Override
    protected void addRuleToMap(Comparable rule, Map map) {
        String[] parts = ((String) rule).split("=");
        map.put(parts[0], parts[1]);
    }

    @Override
    protected String keyFor(Comparable rule) {
        return ((String) rule).split("=")[0];
    }

    @Override
    protected Comparable convertEntryToRule(Entry entry) {
        return entry.getKey() + "=" + entry.getValue();
    }

    @Override
    protected String validateRuleKey(String ruleKey) {
        if (KEYPATTERN.matcher(ruleKey).matches() == false)
            return "Invalid '" + ruleKey + "' not matching " + KEYPATTERN;
        return null;
    }
}
