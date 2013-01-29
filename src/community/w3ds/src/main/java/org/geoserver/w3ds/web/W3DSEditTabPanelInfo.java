/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.web;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ComponentInfo;
import org.geoserver.web.data.resource.LayerEditTabPanel;

public class W3DSEditTabPanelInfo extends ComponentInfo<LayerEditTabPanel> {

	int order = -1;
    GeoServer geoServer;
    
    public GeoServer getGeoServer() {
    	return geoServer;
    }
    
    public void setGeoServer(GeoServer geoServer) {
		this.geoServer = geoServer;
	}

	public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
