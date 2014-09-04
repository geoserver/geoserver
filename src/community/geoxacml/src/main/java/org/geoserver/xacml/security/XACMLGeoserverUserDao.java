/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.User;
import org.geoserver.security.GeoserverUserDao;
import org.geoserver.xacml.geoxacml.GeoXACMLConfig;

public class XACMLGeoserverUserDao extends GeoserverUserDao {

    @Override
    protected User createUserObject(String username, String password, boolean isEnabled,
            GrantedAuthority[] authorities) {
        User user = super.createUserObject(username, password, isEnabled, authorities);
        GeoXACMLConfig.getXACMLRoleAuthority().transformUserDetails(user);
        return user;
    }

}
