/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

/**
 * User property that stores list of groups that a user has {@link GeoServerRole#GROUP_ADMIN_ROLE}
 * privileges for.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GroupAdminProperty extends UserProperty<String[]> {

    private static GroupAdminProperty INSTANCE = new GroupAdminProperty("groups");

    protected GroupAdminProperty(String key) {
        super(key);
    }

    @Override
    public String toString(String[] value) {
        return StringUtils.join(value, ',');
    }

    @Override
    public String[] fromString(String value) {
        return StringUtils.split(value, ',');
    }

    public static String[] get(Properties props) {
        String val = (String) props.get(INSTANCE.getKey());
        return val != null && !"".equals(val) ? INSTANCE.fromString(val) : null;
    }

    public static void set(Properties props, String[] value) {
        props.put(INSTANCE.getKey(), value != null ? INSTANCE.toString(value) : "");
    }

    public static void del(Properties props) {
        props.remove(INSTANCE.getKey());
    }

    public static boolean has(Properties props) {
        return props.containsKey(INSTANCE.getKey());
    }
}
