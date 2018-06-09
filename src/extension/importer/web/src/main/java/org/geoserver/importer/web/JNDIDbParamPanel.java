/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

/**
 * JDNC connection panel.
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class JNDIDbParamPanel extends Panel {
    String jndiReferenceName;
    String schema;

    public JNDIDbParamPanel(String id, String jndiReferenceName) {
        super(id);
        this.jndiReferenceName = jndiReferenceName;

        add(
                new TextField("jndiReferenceName", new PropertyModel(this, "jndiReferenceName"))
                        .setRequired(true));
        add(new TextField("schema", new PropertyModel(this, "schema")));
    }
}
