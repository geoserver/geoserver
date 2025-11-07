/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Creates the following rules, which the test cases expect as precondition:
 *
 * <ul>
 *   <li>1) User: admin - grant ALLOW ALL
 *   <li>2) User: * - grant Service: "WMS" ALLOW
 *   <li>3) * - * DENY
 * </ul>
 */
abstract class AclGeoServerSystemTestSupport extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    protected static XpathEngine xp;

    protected static RuleAdminService ruleAdminService;
    protected static AclResourceAccessManager accessManager;

    protected static AuthorizationService aclAuthorizationService;

    protected static GeoServerDataDirectory dd;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-test.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<>();

        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");

        CiteTestData.registerNamespaces(namespaces);
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        testData.setUp();

        addUser("area", "area", Collections.singletonList("USERS"), Collections.singletonList("ROLE_AUTHENTICATED"));
        addUser("cite", "cite", Collections.singletonList("USERS"), Collections.singletonList("ROLE_AUTHENTICATED"));
        addUser(
                "wms_user",
                "wms_user",
                Collections.singletonList("USERS"),
                Collections.singletonList("ROLE_AUTHENTICATED"));
        addUser("sf", "sf", Collections.singletonList("USERS"), Arrays.asList("ROLE_AUTHENTICATED", "ROLE_SF_ADMIN"));

        catalog = getCatalog();

        // add test properties file to the temporary data dir. For testing purposes only
        dd = new GeoServerDataDirectory(testData.getDataDirectoryRoot());
        GeoServerExtensionsHelper.singleton("dataDirectory", dd, GeoServerDataDirectory.class);

        // get the beans we use for testing
        ruleAdminService = applicationContext.getBean(RuleAdminService.class);
        aclAuthorizationService = applicationContext.getBean(AuthorizationService.class);
        accessManager = applicationContext.getBean(AclResourceAccessManager.class);

        // reset config defaults
        createDefaultRules(ruleAdminService);
    }

    /** subclass hook to register additional namespaces. */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    @After
    public void after() {
        // used by catalog
        logout();

        // used by getAsDOM etc
        super.username = null;
        super.password = null;

        Dispatcher.REQUEST.remove();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
    }

    /**
     * Creates the following rules, which the test cases expect as precondition:
     *
     * <ul>
     *   <li>1) User: admin - grant ALLOW ALL
     *   <li>2) User: * - grant Service: "WMS" ALLOW
     *   <li>3) * - * DENY
     * </ul>
     */
    private void createDefaultRules(RuleAdminService ruleAdminService) {
        List<Rule> rules = ruleAdminService.getAll().collect(Collectors.toList());
        rules.stream().map(Rule::getId).forEach(ruleAdminService::delete);
        ruleAdminService.insert(Rule.allow().withUsername("admin"));
        ruleAdminService.insert(Rule.allow().withService("WMS"));
        ruleAdminService.insert(Rule.deny());
    }

    protected Authentication getUser(String username, String password, String... roles) {

        List<GrantedAuthority> l = new ArrayList<>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        return new UsernamePasswordAuthenticationToken(username, password, l);
    }
}
