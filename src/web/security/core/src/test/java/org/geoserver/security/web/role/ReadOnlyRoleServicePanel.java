/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;

/**
 * Configuration panel for {@link ReadOnlyRoleService}.
 *
 * <p>This service is only used for testing, it is only available when running from the development
 * environment.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ReadOnlyRoleServicePanel extends MemoryRoleServicePanel {

    public ReadOnlyRoleServicePanel(String id, IModel<MemoryRoleServiceConfigImpl> model) {
        super(id, model);
    }
}
