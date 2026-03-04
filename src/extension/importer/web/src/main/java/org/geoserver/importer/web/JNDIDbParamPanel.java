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

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    String jndiReferenceName;
    String schema;

    public JNDIDbParamPanel(String id, String jndiReferenceName) {
        super(id);
        this.jndiReferenceName = jndiReferenceName;

        add(new TextField<>("jndiReferenceName", new PropertyModel<>(this, "jndiReferenceName")).setRequired(true));
        add(new TextField<>("schema", new PropertyModel<>(this, "schema")));
    }
}
