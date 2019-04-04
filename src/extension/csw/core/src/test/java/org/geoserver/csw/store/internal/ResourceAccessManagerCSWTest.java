/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.csw.CSWTestSupport;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ResourceAccessManagerCSWTest extends CSWTestSupport {

    static final Logger LOGGER = Logging.getLogger(ResourceAccessManagerCSWTest.class);

    XpathEngine xpath;

    @Before
    public void setupXpath() {
        this.xpath = XMLUnit.newXpathEngine();
    }

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/csw/store/internal/ResourceAccessManagerContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle("raster", "raster.sld", SystemTestData.class, getCatalog());
        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(
                new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX),
                "raster-filter-test.zip",
                null,
                properties,
                SystemTestData.class,
                getCatalog());

        GeoServerUserGroupStore ugStore =
                getSecurityManager()
                        .loadUserGroupService(AbstractUserGroupService.DEFAULT_NAME)
                        .createStore();

        ugStore.addUser(ugStore.createUserObject("cite", "cite", true));
        ugStore.addUser(ugStore.createUserObject("citeChallenge", "citeChallenge", true));
        ugStore.store();

        GeoServerRoleStore roleStore = getSecurityManager().getActiveRoleService().createStore();
        GeoServerRole role = roleStore.createRoleObject("ROLE_DUMMY");
        roleStore.addRole(role);
        roleStore.associateRoleToUser(role, "cite");
        roleStore.store();

        // populate the access manager
        Catalog catalog = getCatalog();
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");

        // setup hide mode, we should not see the records
        for (ResourceInfo ri : catalog.getResources(ResourceInfo.class)) {
            if (!"cite".equals(ri.getStore().getWorkspace().getName())) {
                tam.putLimits("cite", ri, new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE));
            }
        }

        // setup challenge mode, the metadata should be visible anyways
        for (ResourceInfo ri : catalog.getResources(ResourceInfo.class)) {
            if (!"cite".equals(ri.getStore().getWorkspace().getName())) {
                tam.putLimits(
                        "citeChallenge",
                        ri,
                        new DataAccessLimits(CatalogMode.CHALLENGE, Filter.EXCLUDE));
            }
        }
    }

    @Test
    public void testAllRecordsCite() throws Exception {
        authenticate("cite", "cite");
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        // expected number
        List<ResourceInfo> citeResources =
                getCatalog().getResourcesByNamespace(MockData.CITE_URI, ResourceInfo.class);
        assertEquals(
                citeResources.size(), xpath.getMatchingNodes("//csw:SummaryRecord", d).getLength());
        // check they indeed all start by cite:
        for (ResourceInfo ri : citeResources) {
            assertEquals(
                    1,
                    xpath.getMatchingNodes(
                                    String.format(
                                            "//csw:SummaryRecord[dc:identifier='%s']",
                                            ri.prefixedName()),
                                    d)
                            .getLength());
        }
    }

    @Test
    public void testAllRecordsCiteChallenge() throws Exception {
        // grab before auth to get the full list
        List<ResourceInfo> citeResources = getCatalog().getResources(ResourceInfo.class);
        // authenticate
        authenticate("citeChallenge", "citeChallenge");
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&maxRecords=100";
        Document d = getAsDOM(request);
        // print(d);
        // expected number
        assertEquals(
                citeResources.size(), xpath.getMatchingNodes("//csw:SummaryRecord", d).getLength());
        // check they indeed all start by cite:
        for (ResourceInfo ri : citeResources) {
            assertEquals(
                    1,
                    xpath.getMatchingNodes(
                                    String.format(
                                            "//csw:SummaryRecord[dc:identifier='%s']",
                                            ri.prefixedName()),
                                    d)
                            .getLength());
        }
    }

    protected void authenticate(String username, String password) {
        login(username, password, "MOCKROLE");
    }
}
