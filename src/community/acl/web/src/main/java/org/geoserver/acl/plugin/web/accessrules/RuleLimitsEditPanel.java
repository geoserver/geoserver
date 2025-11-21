/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (factored out from org.geoserver.geofence.server.web.GeofenceRulePage)
 */
package org.geoserver.acl.plugin.web.accessrules;

import org.apache.wicket.model.IModel;
import org.geolatte.geom.MultiPolygon;
import org.geoserver.acl.domain.rules.SpatialFilterType;
import org.geoserver.acl.plugin.web.accessrules.model.MutableRuleLimits;
import org.geoserver.acl.plugin.web.components.AllowedAreaEditPanel;

/** {@link MutableRuleLimits} editor for {@link DataAccessRuleEditPanel} */
@SuppressWarnings("serial")
class RuleLimitsEditPanel extends AllowedAreaEditPanel<MutableRuleLimits> {

    public RuleLimitsEditPanel(String id, IModel<MutableRuleLimits> model) {
        super(id, model, "allowedArea", "spatialFilterType");
    }

    @Override
    public void convertInput() {
        //        super.convertInput();
        MultiPolygon<?> area = getAllowedAreaConvertedInput();
        if (area == null) {
            setConvertedInput(null);
        } else {
            SpatialFilterType type = getSpatialFilterTypeConvertedInput();
            MutableRuleLimits limits = new MutableRuleLimits();
            limits.setAllowedArea(area);
            limits.setSpatialFilterType(type);
            setConvertedInput(limits);
        }
    }
}
