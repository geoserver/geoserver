/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */

package org.geoserver.acl.plugin.it.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.geolatte.geom.jts.JTS;
import org.geoserver.acl.domain.adminrules.AdminGrantType;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.domain.adminrules.AdminRuleIdentifier;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.LayerAttribute;
import org.geoserver.acl.domain.rules.LayerDetails;
import org.geoserver.acl.domain.rules.LayerDetails.Builder;
import org.geoserver.acl.domain.rules.LayerDetails.LayerType;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.domain.rules.RuleIdentifier;
import org.geoserver.acl.domain.rules.RuleLimits;
import org.geoserver.acl.domain.rules.SpatialFilterType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.test.GeoServerTestApplicationContext;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.rules.ExternalResource;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.security.core.context.SecurityContextHolder;

public class AclIntegrationTestSupport extends ExternalResource {

    private RuleAdminService ruleService;
    private AdminRuleAdminService adminRuleService;
    private Supplier<GeoServerTestApplicationContext> appContext;

    public List<LayerGroupInfo> createdLayerGroups;

    public AclIntegrationTestSupport(Supplier<GeoServerTestApplicationContext> appContext) {
        this.appContext = appContext;
    }

    public void setDispatcherRequest(String service, String request) {
        Request req = new Request();
        req.setService(service);
        req.setRequest(request);
        Dispatcher.REQUEST.set(req);
    }

    public @Override void before() {
        createdLayerGroups = new ArrayList<>();

        @SuppressWarnings("PMD.CloseResource")
        GeoServerTestApplicationContext context = appContext.get();
        ruleService = context.getBean(RuleAdminService.class);
        adminRuleService = context.getBean(AdminRuleAdminService.class);

        assertEquals(0, ruleService.count());
        assertEquals(0, adminRuleService.count());
    }

    public @Override void after() {
        deleteLayerGroups();
        deleteRules();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private void deleteLayerGroups() {
        Catalog rawCatalog = getRawCatalog();
        Collections.reverse(createdLayerGroups);
        for (LayerGroupInfo lg : createdLayerGroups) {
            LayerGroupInfo found = rawCatalog.getLayerGroup(lg.getId());
            if (null != found) rawCatalog.remove(found);
        }
    }

    public AdminRule addAdminRule(
            long priority, String username, String rolename, String workspace, AdminGrantType access) {

        AdminRule rule = AdminRule.builder()
                .priority(priority)
                .access(access)
                .identifier(AdminRuleIdentifier.builder()
                        .username(username)
                        .rolename(rolename)
                        .workspace(workspace)
                        .build())
                .build();

        rule = adminRuleService.insert(rule);
        return rule;
    }

    public Rule addRule(
            long priority,
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String workspace,
            String layer) {

        return addRule(priority, access, username, roleName, service, request, null, workspace, layer);
    }

    public Rule addRule(
            long priority,
            GrantType access,
            String username,
            String roleName,
            String service,
            String request,
            String subfield,
            String workspace,
            String layer) {

        RuleIdentifier idf = RuleIdentifier.builder()
                .access(access)
                .username(username)
                .rolename(roleName)
                .service(service)
                .request(request)
                .subfield(subfield)
                .workspace(workspace)
                .layer(layer)
                .build();
        Rule rule = Rule.builder().priority(priority).identifier(idf).build();

        return ruleService.insert(rule);
    }

    public RuleLimits setRuleLimits(Rule rule, CatalogMode mode, MultiPolygon allowedArea, Integer srid)
            throws ParseException {
        return setRuleLimits(rule.getId(), mode, allowedArea.toText(), srid, null);
    }

    public RuleLimits setRuleLimits(Rule rule, CatalogMode mode, String allowedArea, Integer srid)
            throws ParseException {
        return setRuleLimits(rule.getId(), mode, allowedArea, srid, null);
    }

    public RuleLimits setRuleLimits(String ruleId, CatalogMode mode, String allowedArea, Integer srid)
            throws ParseException {
        return setRuleLimits(ruleId, mode, allowedArea, srid, null);
    }

    public RuleLimits setRuleLimits(
            Rule rule, CatalogMode mode, String allowedArea, Integer srid, SpatialFilterType spatialFilterType)
            throws org.locationtech.jts.io.ParseException {

        return setRuleLimits(rule.getId(), mode, allowedArea, srid, spatialFilterType);
    }

    public RuleLimits setRuleLimits(
            String ruleId, CatalogMode mode, String allowedArea, Integer srid, SpatialFilterType spatialFilterType)
            throws org.locationtech.jts.io.ParseException {

        MultiPolygon allowedAreaGeom = (MultiPolygon) new WKTReader().read(allowedArea);
        if (srid != null) allowedAreaGeom.setSRID(srid);
        if (spatialFilterType == null) spatialFilterType = SpatialFilterType.INTERSECT;

        org.geolatte.geom.MultiPolygon<?> area = JTS.from(allowedAreaGeom);
        RuleLimits limits = RuleLimits.builder()
                .allowedArea(area)
                .spatialFilterType(spatialFilterType)
                .catalogMode(mode)
                .build();
        ruleService.setLimits(ruleId, limits);
        return limits;
    }

    public LayerDetails setCqlReadFilter(Rule allowRule, String cql) {
        Builder builder = ruleService
                .getLayerDetails(allowRule)
                .map(LayerDetails::toBuilder)
                .orElseGet(LayerDetails::builder);
        LayerDetails layerDetails = builder.cqlFilterRead(cql).build();
        ruleService.setLayerDetails(allowRule.getId(), layerDetails);
        return layerDetails;
    }

    public LayerDetails setCqlWriteFilter(Rule allowRule, String cql) {
        Builder builder = ruleService
                .getLayerDetails(allowRule)
                .map(LayerDetails::toBuilder)
                .orElseGet(LayerDetails::builder);
        LayerDetails layerDetails = builder.cqlFilterWrite(cql).build();
        ruleService.setLayerDetails(allowRule.getId(), layerDetails);
        return layerDetails;
    }

    public LayerDetails setLayerDetails(
            Rule rule,
            Set<String> allowedStyles,
            Set<LayerAttribute> attributes,
            CatalogMode mode,
            String cqlRead,
            String cqlWrite,
            LayerType layerType) {
        return setLayerDetails(rule.getId(), allowedStyles, attributes, mode, cqlRead, cqlWrite, layerType);
    }

    public LayerDetails setLayerDetails(
            String ruleId,
            Set<String> allowedStyles,
            Set<LayerAttribute> attributes,
            CatalogMode mode,
            String cqlRead,
            String cqlWrite,
            LayerType layerType) {
        LayerDetails details = LayerDetails.builder()
                .type(layerType)
                .attributes(attributes)
                .allowedStyles(allowedStyles)
                .catalogMode(mode)
                .cqlFilterRead(cqlRead)
                .cqlFilterWrite(cqlWrite)
                .build();

        ruleService.setLayerDetails(ruleId, details);
        return details;
    }

    public void deleteRules() {
        ruleService.getAll().map(Rule::getId).forEach(ruleService::delete);
        adminRuleService.getAll().map(AdminRule::getId).forEach(adminRuleService::delete);
        assertEquals(0, ruleService.count());
        assertEquals(0, adminRuleService.count());
    }

    public void deleteRules(String... ids) {
        Arrays.stream(ids).forEach(ruleService::delete);
    }

    public Catalog getRawCatalog() {
        return appContext.get().getBean("rawCatalog", Catalog.class);
    }

    public LayerGroupInfo createLayerGroup(
            String name, LayerGroupInfo.Mode mode, LayerInfo rootLayer, LayerInfo... layers) throws Exception {

        return createLayerGroup(name, mode, rootLayer, Arrays.asList(layers), null);
    }

    public LayerGroupInfo createLayerGroup(
            String name,
            LayerGroupInfo.Mode mode,
            LayerInfo rootLayer,
            List<LayerInfo> layers,
            CoordinateReferenceSystem groupCRS)
            throws Exception {

        Catalog catalog = getRawCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);

        group.setMode(mode);
        if (rootLayer != null) {
            group.setRootLayer(rootLayer);
            group.setRootLayerStyle(rootLayer.getDefaultStyle());
        }
        for (LayerInfo li : layers) group.getLayers().add(li);
        group.getStyles().add(null);
        group.getStyles().add(null);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.calculateLayerGroupBounds(group);
        if (groupCRS != null) {
            ReferencedEnvelope re = group.getBounds();
            MathTransform transform = CRS.findMathTransform(group.getBounds().getCoordinateReferenceSystem(), groupCRS);
            Envelope bbox = org.geotools.geometry.jts.JTS.transform(re, transform);
            ReferencedEnvelope newRe =
                    new ReferencedEnvelope(bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY(), groupCRS);
            group.setBounds(newRe);
        }
        catalog.add(group);
        group = catalog.getLayerGroup(group.getId());
        assertNotNull(group);
        this.createdLayerGroups.add(group);
        return group;
    }
}
