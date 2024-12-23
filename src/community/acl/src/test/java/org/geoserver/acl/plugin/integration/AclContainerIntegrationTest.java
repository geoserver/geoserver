/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.integration;

import static java.lang.String.format;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.domain.rules.RuleIdentifier;
import org.geoserver.acl.testcontainer.GeoServerAclContainer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Runs the {@link GeoServerAclContainer} test container in dev mode (that is, with an embedded h2 database) on a random
 * port, sets up the GeoServer-ACL api client system properties to work against the ephemeral service, and runs some
 * plugin integration tests.
 */
public class AclContainerIntegrationTest extends GeoServerSystemTestSupport {

    @ClassRule
    public static final GeoServerAclContainer aclServer =
            GeoServerAclContainer.currentVersion().withDevMode().disabledWithoutDocker();

    @BeforeClass
    public static void setUpAclEnvironment() {
        String apiUrl = aclServer.apiUrl();
        String user = aclServer.devAdminUser();
        String pwd = aclServer.devAdminPassword();

        System.setProperty("geoserver.acl.enabled", "true"); // default value
        System.setProperty("geoserver.acl.client.basePath", apiUrl);
        System.setProperty("geoserver.acl.client.username", user);
        System.setProperty("geoserver.acl.client.password", pwd);
        // fail fast if the server is not available
        System.setProperty("geoserver.acl.client.initTimeout", "3");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wms", "http://www.opengis.net/wms");
        SystemTestData.registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    private final List<String> cdfLayers = List.of(
            "cdf:Deletes",
            "cdf:Fifteen",
            "cdf:Inserts",
            "cdf:Locks",
            "cdf:Nulls",
            "cdf:Other",
            "cdf:Seven",
            "cdf:Updates");
    private final List<String> cgfLayers =
            List.of("cgf:Lines", "cgf:MLines", "cgf:MPoints", "cgf:MPolygons", "cgf:Points", "cgf:Polygons");
    private final List<String> citeLayers = List.of(
            "cite:BasicPolygons",
            "cite:Bridges",
            "cite:Buildings",
            "cite:DividedRoutes",
            "cite:Forests",
            "cite:Lakes",
            "cite:MapNeatline",
            "cite:NamedPlaces",
            "cite:Ponds",
            "cite:RoadSegments",
            "cite:Streams");
    private final List<String> sfLayers =
            List.of("sf:AggregateGeoFeature", "sf:GenericEntity", "sf:PrimitiveGeoFeature");
    private final List<String> allLayers = Stream.of(cdfLayers, cgfLayers, citeLayers, sfLayers)
            .flatMap(List::stream)
            .collect(Collectors.toList());

    @Test
    public void aclIntegrationTest() throws Exception {
        assertAdminDefaultToSeeAll();
        assertAnonymousDefaultsToHideAll();
        assertNonAdminDefaultsToHideAll();

        createRoleRule("ROLE_CITE", ALLOW, "cite", null);
        createRoleRule("ROLE_CDF", ALLOW, "cdf", null);
        createRoleRule("ROLE_CGF", ALLOW, "cgf", null);
        createRoleRule("ROLE_SF", ALLOW, "sf", null);

        createRoleRule("ROLE_POLYGONS", ALLOW, "cgf", "MPolygons");
        createRoleRule("ROLE_POLYGONS", ALLOW, "cgf", "Polygons");
        createRoleRule("ROLE_POLYGONS", ALLOW, "cite", "BasicPolygons");

        login("cite", "pwd", "ROLE_CITE");
        assertVisible(citeLayers);
        assertNotVisible(cdfLayers, cgfLayers, sfLayers);

        login("cdf", "pwd", "ROLE_CDF");
        assertVisible(cdfLayers);
        assertNotVisible(citeLayers, cgfLayers, sfLayers);

        login("sf", "pwd", "ROLE_CDF");
        assertVisible(cdfLayers);
        assertNotVisible(citeLayers, cgfLayers, sfLayers);

        login("multiroleuser", "pwd", "ROLE_CGF", "ROLE_SF");
        assertVisible(cgfLayers, sfLayers);
        assertNotVisible(citeLayers, cdfLayers);

        login("individuallayersuser", "pwd", "ROLE_POLYGONS");
        List<String> polyLayers = List.of("cgf:MPolygons", "cgf:Polygons", "cite:BasicPolygons");
        assertVisible(polyLayers);
        assertNotVisible(remove(allLayers, polyLayers));

        login("norolematchuser", "pwd", "ROLE_USER");
        assertNotVisible(allLayers);

        createUserRule("thauser", ALLOW, "sf", null);
        login("thauser", "pwd", "ROLE_USER");
        assertVisible(sfLayers);
        assertNotVisible(cgfLayers, cdfLayers, citeLayers);

        // matches user and role based rules
        login("thauser", "pwd", "ROLE_USER", "ROLE_CITE");
        assertVisible(sfLayers, citeLayers);
        assertNotVisible(cgfLayers, cdfLayers);
    }

    private List<String> remove(List<String> from, List<String> remove) {
        return from.stream().filter(l -> !remove.contains(l)).collect(Collectors.toList());
    }

    private void createRoleRule(String role, GrantType grant, String workspace, String layer) {
        createRule(null, role, grant, workspace, layer);
    }

    private void createUserRule(String user, GrantType grant, String workspace, String layer) {
        createRule(user, null, grant, workspace, layer);
    }

    private void createRule(String user, String role, GrantType grant, String workspace, String layer) {
        RuleAdminService ruleAdminService = applicationContext.getBean(RuleAdminService.class);
        RuleIdentifier identifier = RuleIdentifier.builder()
                .username(user)
                .rolename(role)
                .workspace(workspace)
                .layer(layer)
                .access(grant)
                .build();
        Rule rule = Rule.builder().identifier(identifier).build();
        ruleAdminService.insert(rule);
    }

    private void assertAdminDefaultToSeeAll() throws Exception {
        loginAsAdmin();
        assertVisible(allLayers);
    }

    private void assertAnonymousDefaultsToHideAll() throws Exception {
        loginAsAnonymous();
        assertNotVisible(allLayers);
    }

    private void assertNonAdminDefaultsToHideAll() throws Exception {
        login("someuser", "pwd", "ROLE_USER", "ROLE_SF", "ROLE_AUTHENTICATED");
        assertNotVisible(allLayers);
    }

    @SafeVarargs
    private void assertVisible(List<String>... layers) throws Exception {
        List<String> expected = Stream.of(layers).flatMap(List::stream).collect(Collectors.toList());
        assertWMSCapabilitiesContainsAll(expected);
        assertWFSCapabilitiesContainsAll(expected);
    }

    @SafeVarargs
    private void assertNotVisible(List<String>... layers) throws Exception {
        List<String> expected = Stream.of(layers).flatMap(List::stream).collect(Collectors.toList());
        assertWMSCapabilitiesDoesNotContain(expected);
        assertWFSCapabilitiesDoesNotContain(expected);
    }

    private void assertWMSCapabilitiesContainsAll(List<String> layers) throws Exception {
        Document wmsCaps = getWmsCapabilities();
        Function<String, String> xpathBuilder = layer -> format("//wms:Layer/wms:Name[text() = '%s']", layer);
        assertXpathExists(wmsCaps, xpathBuilder, layers);
    }

    private void assertWMSCapabilitiesDoesNotContain(List<String> layers) throws Exception {
        Document wmsCaps = getWmsCapabilities();
        Function<String, String> xpathBuilder = layer -> format("//wms:Layer/wms:Name[text() = '%s']", layer);
        assertXpathDoesNotExist(wmsCaps, xpathBuilder, layers);
    }

    private Document getWmsCapabilities() throws Exception, XpathException {
        Document wmsCaps = super.getAsDOM("/wms?version=1.3.0&request=GetCapabilities");
        XMLAssert.assertXpathExists("/wms:WMS_Capabilities", wmsCaps);
        return wmsCaps;
    }

    private void assertWFSCapabilitiesContainsAll(List<String> layers) throws Exception {
        Document wfsCaps = getWfsCapabilities();
        Function<String, String> xpathBuilder = layer -> format("//wfs:FeatureType/wfs:Name[text() = '%s']", layer);
        assertXpathExists(wfsCaps, xpathBuilder, layers);
    }

    private void assertWFSCapabilitiesDoesNotContain(List<String> layers) throws Exception {
        Document wfsCaps = getWfsCapabilities();
        Function<String, String> xpathBuilder = layer -> format("//wfs:FeatureType/wfs:Name[text() = '%s']", layer);
        assertXpathDoesNotExist(wfsCaps, xpathBuilder, layers);
    }

    private void assertXpathExists(Document dom, Function<String, String> xpathBuilder, List<String> layers)
            throws Exception {
        for (String layer : layers) {
            String xpath = xpathBuilder.apply(layer);
            XMLAssert.assertXpathExists(xpath, dom);
        }
    }

    private void assertXpathDoesNotExist(Document dom, Function<String, String> xpathBuilder, List<String> layers)
            throws Exception {
        for (String layer : layers) {
            String xpath = xpathBuilder.apply(layer);
            XMLAssert.assertXpathNotExists(xpath, dom);
        }
    }

    private Document getWfsCapabilities() throws Exception, XpathException {
        Document wfsCaps = super.getAsDOM("/wfs?version=1.1.0&request=GetCapabilities");
        XMLAssert.assertXpathExists("/wfs:WFS_Capabilities", wfsCaps);
        return wfsCaps;
    }

    public void loginAsAdmin() {
        login("admin", "geoserver", "ADMIN", "ROLE_ADMINISTRATOR");
    }

    public void loginAsAnonymous() {
        login("anonymous", "", "ROLE_ANONYMOUS");
    }
}
