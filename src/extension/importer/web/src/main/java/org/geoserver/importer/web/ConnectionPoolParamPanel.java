/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

/** Connection pool related parameter form */
@SuppressWarnings("serial")
class ConnectionPoolParamPanel extends Panel {

    private static final boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(
            java.lang.invoke.MethodHandles.lookup().lookupClass());

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

    int minConnection = 1;
    int maxConnection = 10;
    int fetchSize = 1000;
    int timeout = 20;
    boolean validate = true;
    boolean preparedStatements = true;

    public ConnectionPoolParamPanel(String id, boolean preparedStatements) {
        super(id);

        add(new TextField<>("minConnection", new PropertyModel<>(this, "minConnection")).setRequired(true));
        add(new TextField<>("maxConnection", new PropertyModel<>(this, "maxConnection")).setRequired(true));
        add(new TextField<>("fetchSize", new PropertyModel<>(this, "fetchSize")).setRequired(true));
        add(new TextField<>("timeout", new PropertyModel<>(this, "timeout")).setRequired(true));
        add(new CheckBox("validate", new PropertyModel<>(this, "validate")));
        WebMarkupContainer psContainer = new WebMarkupContainer("psContainer");
        psContainer.add(new CheckBox("preparedStatements", new PropertyModel<>(this, "preparedStatements")));
        psContainer.setVisible(preparedStatements);
        add(psContainer);
    }
}
