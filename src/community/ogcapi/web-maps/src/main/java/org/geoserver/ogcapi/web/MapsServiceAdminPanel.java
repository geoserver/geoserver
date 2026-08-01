/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.ogcapi.CQL2Conformance;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.ogcapi.ECQLConformance;
import org.geoserver.ogcapi.v1.maps.MapsConformance;
import org.geoserver.ogcapi.v1.maps.MapsSettings;
import org.geoserver.web.ogcapi.ConformanceTable;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.wms.WMSInfo;

/** WMS service admin panel to enable or disable the optional OGC API - Maps conformance classes. */
public class MapsServiceAdminPanel extends AdminPagePanel {

    public MapsServiceAdminPanel(String id, IModel<?> info) {
        super(id, info);
        // resolve the configuration from the live WMSInfo on each access: the service is reloaded per request
        IModel<ConformanceInfo<?>> maps = IModel.of(() -> MapsConformance.configuration((WMSInfo) info.getObject()));
        add(new ConformanceTable("mapsConformance", maps, this));

        // how many collections a dataset map draws by itself is a setting rather than a conformance class, so it has
        // its own WMS metadata entry and sits beside the table
        NumberTextField<Integer> defaultCollections = new NumberTextField<>(
                "defaultCollections",
                new MetadataMapModel<>(
                        new PropertyModel<>(info, "metadata"), MapsSettings.DEFAULT_COLLECTIONS_KEY, Integer.class),
                Integer.class);
        defaultCollections.setMinimum(1);
        add(defaultCollections);
        add(new ConformanceTable(
                "cqlConformance", IModel.of(() -> CQL2Conformance.configuration((WMSInfo) info.getObject())), this));
        add(new ConformanceTable(
                "ecqlConformance", IModel.of(() -> ECQLConformance.configuration((WMSInfo) info.getObject())), this));
    }
}
