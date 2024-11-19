package org.geoserver.ogcapi.web;

import org.apache.wicket.model.IModel;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.v1.features.FeatureService;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.wfs.WFSInfo;

import java.util.List;

public class ConformanceAdminPanel extends AdminPagePanel {

        public ConformanceAdminPanel(String id, IModel<?> model) {
            super(id, model);
            WFSInfo wfsInfo = (WFSInfo) model.getObject();
            FeatureService featureService = GeoServerExtensions.bean(FeatureService.class);
            List<APIConformance> available = featureService.getConformances();
            // ...
        }
}
