/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.geoserver.mapml.MapMLConstants;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.wms.WMSInfo;

/** Admin panel for MapML service. */
public class MapMLAdminPanel extends AdminPagePanel {

    private static final long serialVersionUID = -7670555379263411393L;

    /**
     * Constructor
     *
     * @param id component id
     * @param model model
     */
    public MapMLAdminPanel(String id, IModel<?> model) {
        super(id, model);
        WMSInfo wmsInfo = (WMSInfo) model.getObject();
        CheckBox multiextent = new CheckBox(
                "multiextent", new MapModel<>(wmsInfo.getMetadata(), MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT));
        this.add(new Component[] {multiextent});
    }
}
