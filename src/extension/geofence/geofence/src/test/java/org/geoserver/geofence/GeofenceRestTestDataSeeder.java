/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import org.geofence.web.rest.api.model.RESTInputRule;
import org.geofence.web.rest.api.model.RESTLayerConstraints;
import org.geofence.web.rest.api.model.RESTRulePosition;
import org.geofence.web.rest.api.model.RESTRulePosition.RESTPositionReference;
import org.geofence.web.rest.api.model.enums.RESTGrantType;
import org.geofence.web.rest.api.model.enums.RESTSpatialFilterType;
import org.geofence.web.rest.client.GeoFenceAdminClient;

/**
 * Wipes and re-seeds, via the REST admin client, the exact rule fixture {@code GeofenceAccessManagerTest}/
 * {@code ServicesTest}/{@code GeofenceAccessManager_WMTSLayerTest} assert against - a faithful port of upstream
 * geoserver/geofence's original {@code MainTest} fixture
 * ({@code src/services/core/webtest/.../servicetest/MainTest.java}), previously seeded server-side by a now-removed
 * dedicated test webapp module (geofence_39's {@code web/rest/test}).
 *
 * <p>Doubles as live test coverage for {@link GeoFenceAdminClient}, which otherwise only has its own live-server
 * integration test that self-skips without a running server.
 */
class GeofenceRestTestDataSeeder {

    private final GeoFenceAdminClient client;

    GeofenceRestTestDataSeeder(String restUrl) {
        client = new GeoFenceAdminClient();
        client.setRestUrl(restUrl);
    }

    void seed() {
        client.removeAll();

        long priority = 0;

        // cite -> full ALLOW on the whole "cite" workspace
        RESTInputRule citeWorkspace = rule(priority++, RESTGrantType.ALLOW);
        citeWorkspace.setUsername("cite");
        citeWorkspace.setWorkspace("cite");
        insert(citeWorkspace);

        // cite -> WMS GetMap/GetCapabilities/reflect only, on the "sf" workspace
        insert(citeSfRule(priority++, "GetMap"));
        insert(citeSfRule(priority++, "GetCapabilities"));
        insert(citeSfRule(priority++, "reflect"));

        // wmsuser -> WMS only, any workspace/layer
        RESTInputRule wmsuser = rule(priority++, RESTGrantType.ALLOW);
        wmsuser.setUsername("wmsuser");
        wmsuser.setService("wms");
        insert(wmsuser);

        // area -> ALLOW on sf:GenericEntity, INTERSECT-restricted to a specific allowed area. Upstream's MainTest
        // uses a rule-level RuleLimits.allowedArea (any workspace/layer) instead, via a separate LIMIT rule - the
        // REST API only exposes area restriction through RESTLayerConstraints (-> LayerDetails), which requires a
        // fixed layer, so this is scoped to sf:GenericEntity, the only layer the "area" tests actually assert
        // against. spatialFilterType=INTERSECT (not the default CLIP) is what makes LayerDetails.area produce the
        // same intersects-style filter RuleLimits.allowedArea would - clip vs intersect is carried by
        // spatialFilterType, not by which of the two classes holds the area.
        RESTInputRule area = rule(priority++, RESTGrantType.ALLOW);
        area.setUsername("area");
        area.setWorkspace("sf");
        area.setLayer("GenericEntity");
        RESTLayerConstraints constraints = new RESTLayerConstraints();
        constraints.setRestrictedAreaWkt("MULTIPOLYGON(((48 62, 48 63, 49 63, 49 62, 48 62)))");
        constraints.setSpatialFilterType(RESTSpatialFilterType.INTERSECT);
        area.setConstraints(constraints);
        insert(area);

        // u-states -> full ALLOW, but only on topp:states
        RESTInputRule states = rule(priority++, RESTGrantType.ALLOW);
        states.setUsername("u-states");
        states.setWorkspace("topp");
        states.setLayer("states");
        insert(states);

        // catch-all -> DENY (lowest priority, evaluated last)
        insert(rule(priority++, RESTGrantType.DENY));
    }

    private RESTInputRule citeSfRule(long priority, String request) {
        RESTInputRule r = rule(priority, RESTGrantType.ALLOW);
        r.setUsername("cite");
        r.setService("wms");
        r.setRequest(request);
        r.setWorkspace("sf");
        return r;
    }

    private void insert(RESTInputRule rule) {
        client.getRuleService().insert(rule);
    }

    private static RESTInputRule rule(long priority, RESTGrantType grant) {
        RESTInputRule rule = new RESTInputRule();
        rule.setPosition(new RESTRulePosition(RESTPositionReference.fixedPriority, priority));
        rule.setGrant(grant);
        return rule;
    }
}
