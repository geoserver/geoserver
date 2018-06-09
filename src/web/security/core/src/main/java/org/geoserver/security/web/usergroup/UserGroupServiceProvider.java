/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.web.SecurityNamedServiceProvider;

/**
 * Data provider for user group service configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupServiceProvider
        extends SecurityNamedServiceProvider<SecurityUserGroupServiceConfig> {

    public static final Property<SecurityUserGroupServiceConfig> PWD_ENCODER =
            new ResourceBeanProperty("passwordEncoderName", "passwordEncoderName");

    public static final Property<SecurityUserGroupServiceConfig> PWD_POLICY =
            new ResourceBeanProperty("passwordPolicyName", "passwordPolicyName");

    @Override
    protected List<SecurityUserGroupServiceConfig> getItems() {
        List<SecurityUserGroupServiceConfig> result =
                new ArrayList<SecurityUserGroupServiceConfig>();
        try {
            for (String name : getSecurityManager().listUserGroupServices()) {
                result.add(getSecurityManager().loadUserGroupServiceConfig(name));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    @Override
    protected List<Property<SecurityUserGroupServiceConfig>> getProperties() {
        List props = new ArrayList(super.getProperties());
        props.add(PWD_ENCODER);
        props.add(PWD_POLICY);
        return props;
    }
}
