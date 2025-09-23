/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.web;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.convert.IConverter;

/** Converts roles list as ";" separated string */
public class RolesConverter implements IConverter {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -7814332099119849464L;

    private List<String> availableRoles;

    public RolesConverter(List<String> availableRoles) {
        this.availableRoles = availableRoles;
    }

    @Override
    public Object convertToObject(String value, Locale locale) {
        List<String> checkedRoles = new ArrayList<>();
        if (value != null && !value.isEmpty()) {
            String[] selectedRoles = value.split(";");
            // Check roles string
            for (String role : selectedRoles) {
                if (availableRoles.contains(role)) {
                    checkedRoles.add(role);
                }
            }
        }
        return checkedRoles;
    }

    @Override
    public String convertToString(Object value, Locale locale) {
        String roleStr = "";
        if (value != null && value instanceof List roles) {
            roleStr = StringUtils.join(roles.toArray(), ";");
            if (!roleStr.isEmpty()) {
                roleStr = roleStr + ";";
            }
        }
        return roleStr;
    }
}
