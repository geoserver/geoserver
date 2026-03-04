/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jdbcconfig.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;

/** @author Kevin Smith, OpenGeo */
public class JDBCConfigStatusPanel extends Panel {

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

    public JDBCConfigStatusPanel(String id, JDBCConfigProperties config) {
        super(id);
        this.add(new Label("jdbcConfigDatasourceId", config.getDatasourceId()));
    }
}
