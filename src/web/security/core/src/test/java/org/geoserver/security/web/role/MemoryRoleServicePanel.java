/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;

/**
 * Configuration panel for {@link MemoryRoleService}.
 * <p>
 * This service is only used for testing, it is only available when running from the development 
 * environment. 
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class MemoryRoleServicePanel extends RoleServicePanel<MemoryRoleServiceConfigImpl> {

    public MemoryRoleServicePanel(String id,
            IModel<MemoryRoleServiceConfigImpl> model) {
        super(id, model);

        add(new TextField("toBeEncrypted"));
    }

}
