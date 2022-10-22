/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */

package org.geoserver.geofence.server.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.geoserver.geofence.cache.CachedRuleReader;
import org.geoserver.geofence.core.model.AdminRule;
import org.geoserver.geofence.core.model.GSInstance;
import org.geoserver.geofence.core.model.IPAddressRange;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.AdminGrantType;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.SpatialFilterType;
import org.geoserver.geofence.services.AdminRuleAdminService;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestApplicationContext;
import org.junit.rules.ExternalResource;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeofenceIntegrationTestSupport extends ExternalResource {

    private RuleAdminService ruleService;
    private AdminRuleAdminService adminRuleService;
    private Supplier<GeoServerTestApplicationContext> appContext;

    private List<Long> ruleIds;
    private List<Long> adminRuleIds;

    public GeofenceIntegrationTestSupport(Supplier<GeoServerTestApplicationContext> appContext) {
        this.appContext = appContext;
    }

    public @Override void before() {
        ruleIds = new ArrayList<>();
        adminRuleIds = new ArrayList<>();
        @SuppressWarnings("PMD.CloseResource")
        GeoServerTestApplicationContext context = appContext.get();
        ruleService = context.getBean(RuleAdminService.class);
        adminRuleService = context.getBean(AdminRuleAdminService.class);
    }

    public @Override void after() {
        deleteRules();
        // this is odd, RuleService.delete() should invalidate
        CachedRuleReader cacheRuleReader = GeoServerExtensions.bean(CachedRuleReader.class);
        cacheRuleReader.invalidateAll();
    }

    public long addAdminRule(
            long priority,
            String username,
            String rolename,
            String workspace,
            AdminGrantType access) {
        GSInstance gsInstance = null;
        IPAddressRange addressRange = null;
        AdminRule rule =
                new AdminRule(
                        priority, username, rolename, gsInstance, addressRange, workspace, access);
        long id = adminRuleService.insert(rule);
        this.adminRuleIds.add(id);
        return id;
    }

    public long addRule(
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String workspace,
            String layer,
            long priority) {

        return addRule(
                access, username, roleName, service, request, null, workspace, layer, priority);
    }

    public long addRule(
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
        long id = ruleService.insert(rule);
        ruleIds.add(id);
        return id;
    }

    public void addRuleLimits(long ruleId, CatalogMode mode, String allowedArea, Integer srid)
            throws ParseException {
        addRuleLimits(ruleId, mode, allowedArea, srid, null);
    }

    public void addRuleLimits(
            long ruleId,
            CatalogMode mode,
            String allowedArea,
            Integer srid,
            SpatialFilterType spatialFilterType)
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

    public void deleteRules() {
        ruleIds.forEach(ruleService::delete);
        adminRuleIds.forEach(adminRuleService::delete);
    }

    public void deleteRules(Long... ids) {
        for (Long id : ids) {
            if (id != null) ruleService.delete(id);
        }
    }
}
