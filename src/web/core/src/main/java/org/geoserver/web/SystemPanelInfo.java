/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.SystemInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
/**
 * Report status of system.
 * 
 * @author sandr
 *
 */
public class SystemPanelInfo<T extends SystemInfo> extends ComponentInfo<Panel> {

    private static final long serialVersionUID = -1926519578555796146L;
    
    private Integer order;
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public Integer getOrder() {
        return order;
    }
    
}
