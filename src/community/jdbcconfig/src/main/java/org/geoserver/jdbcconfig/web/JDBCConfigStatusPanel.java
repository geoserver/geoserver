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

    public JDBCConfigStatusPanel(String id, JDBCConfigProperties config) {
        super(id);
        this.add(new Label("jdbcConfigDatasourceId", config.getDatasourceId()));
    }
}
