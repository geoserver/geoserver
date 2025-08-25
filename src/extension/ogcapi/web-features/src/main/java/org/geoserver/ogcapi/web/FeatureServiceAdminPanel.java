/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import org.apache.wicket.model.IModel;
import org.geoserver.ogcapi.v1.features.CQL2Conformance;
import org.geoserver.ogcapi.v1.features.ECQLConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.wfs.WFSInfo;

public class FeatureServiceAdminPanel extends AdminPagePanel {

    @Override
    public void onMainFormSubmit() {
        WFSInfo wfsInfo = (WFSInfo) getDefaultModel().getObject();
        FeatureConformance features = FeatureConformance.configuration(wfsInfo);
        CQL2Conformance cql2 = CQL2Conformance.configuration(wfsInfo);
        ECQLConformance ecql = ECQLConformance.configuration(wfsInfo);

        if (!features.isEnabled(wfsInfo)) {
            wfsInfo.getMetadata().remove(FeatureConformance.METADATA_KEY);
        }
        if (!cql2.isEnabled(wfsInfo)) {
            wfsInfo.getMetadata().remove(CQL2Conformance.METADATA_KEY);
        }
        if (!ecql.isEnabled(wfsInfo)) {
            wfsInfo.getMetadata().remove(ECQLConformance.METADATA_KEY);
        }
    }

    public FeatureServiceAdminPanel(String id, final IModel<?> info) {
        super(id, info);
        WFSInfo wfsInfo = (WFSInfo) info.getObject();
        add(new ConformanceTable("featureConformance", FeatureConformance.configuration(wfsInfo), this));
        add(new ConformanceTable("cqlConformance", CQL2Conformance.configuration(wfsInfo), this));
        add(new ConformanceTable("ecqlConformance", ECQLConformance.configuration(wfsInfo), this));
    }
}
