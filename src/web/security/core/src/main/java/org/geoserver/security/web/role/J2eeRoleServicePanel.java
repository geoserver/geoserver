/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.J2eeRoleServiceConfig;

/**
 * Configuration panel for {@link J2eeRoleServiceConfig}.
 *
 * @author christian
 */
public class J2eeRoleServicePanel extends RoleServicePanel<J2eeRoleServiceConfig> {

    private static final long serialVersionUID = 1L;

    public J2eeRoleServicePanel(String id, IModel<J2eeRoleServiceConfig> model) {
        super(id, model);
    }
}
