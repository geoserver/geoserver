/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.web;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

/**
 * 
 * Panel to visualize system informations
 * @author sandr
 *
 */

@Component
public class SystemStatusMonitor extends Panel {

    private static final long serialVersionUID = -5616622546856772557L;

    public SystemStatusMonitor(String id) {
        super(id);
    }

}
