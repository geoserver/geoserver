/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.integration;

import java.util.Set;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.geofence.core.model.LayerDetails;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.LayerType;
import org.geoserver.geofence.core.model.enums.SpatialFilterType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeofenceWMSTestSupport extends WMSTestSupport {

    protected RuleAdminService ruleService;

    @Before
    public void before() {
        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
    }

    static long addRule(
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String workspace,
            String layer,
            long priority,
            RuleAdminService ruleService) {

        Rule rule = new Rule();
        rule.setAccess(access);
        rule.setUsername(username);
        rule.setRolename(roleName);
        rule.setService(service);
        rule.setRequest(request);
        rule.setWorkspace(workspace);
        rule.setLayer(layer);
        rule.setPriority(priority);
        return ruleService.insert(rule);
    }

    long addRule(
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String subfield,
            String workspace,
            String layer,
            long priority) {

        Rule rule = new Rule();
        rule.setAccess(access);
        rule.setUsername(username);
        rule.setRolename(roleName);
        rule.setService(service);
        rule.setRequest(request);
        rule.setSubfield(subfield);
        rule.setWorkspace(workspace);
        rule.setLayer(layer);
        rule.setPriority(priority);
        return ruleService.insert(rule);
    }

    static void addRuleLimits(
            long ruleId,
            CatalogMode mode,
            String allowedArea,
            Integer srid,
            RuleAdminService ruleService)
            throws ParseException {
        addRuleLimits(ruleId, mode, allowedArea, srid, null, ruleService);
    }

    static void addRuleLimits(
            long ruleId,
            CatalogMode mode,
            String allowedArea,
            Integer srid,
            SpatialFilterType spatialFilterType,
            RuleAdminService ruleService)
            throws org.locationtech.jts.io.ParseException {
        RuleLimits limits = new RuleLimits();
        limits.setCatalogMode(mode);
        MultiPolygon allowedAreaGeom = (MultiPolygon) new WKTReader().read(allowedArea);
        if (srid != null) allowedAreaGeom.setSRID(srid);
        limits.setAllowedArea(allowedAreaGeom);
        if (spatialFilterType == null) spatialFilterType = SpatialFilterType.INTERSECT;
        limits.setSpatialFilterType(spatialFilterType);
        ruleService.setLimits(ruleId, limits);
    }

    static void deleteRules(RuleAdminService ruleService, Long... ids) {
        for (Long id : ids) {
            if (id != null) ruleService.delete(id);
        }
    }

    protected LayerGroupInfo addLakesPlacesLayerGroup(LayerGroupInfo.Mode mode, String name)
            throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerGroupInfo group = createLakesPlacesLayerGroup(getCatalog(), name, mode, null);
        logout();
        return group;
    }

    protected void removeLayerGroup(LayerGroupInfo... groups) {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        for (LayerGroupInfo group : groups) {
            if (group != null) {
                getCatalog().remove(group);
            }
        }
        logout();
    }

    static void addLayerDetails(
            RuleAdminService ruleService,
            Long ruleId,
            Set<String> allowedStyles,
            Set<LayerAttribute> attributes,
            CatalogMode mode,
            String cqlRead,
            String cqlWrite,
            LayerType layerType)
            throws org.locationtech.jts.io.ParseException {
        LayerDetails details = new LayerDetails();
        details.setType(layerType);
        details.setAttributes(attributes);
        details.setAllowedStyles(allowedStyles);
        details.setCatalogMode(mode);
        details.setCqlFilterWrite(cqlWrite);
        details.setCqlFilterRead(cqlRead);
        ruleService.setDetails(ruleId, details);
    }
}
