/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;

/**
 * Configuration panel for {@link XMLUserGroupService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XMLUserGroupServicePanel extends UserGroupServicePanel<XMLUserGroupServiceConfig> {

    public XMLUserGroupServicePanel(String id, IModel<XMLUserGroupServiceConfig> model) {
        super(id, model);

        add(new TextField("fileName").setEnabled(isNew()));
        add(new CheckBox("validating"));
        add(new TextField("checkInterval"));
    }
}
