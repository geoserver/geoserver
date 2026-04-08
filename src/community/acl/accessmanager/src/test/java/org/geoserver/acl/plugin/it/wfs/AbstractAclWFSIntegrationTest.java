/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.it.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.net.InetAddress;
import java.util.List;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManager;
import org.geoserver.acl.plugin.it.support.AclIntegrationTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.WFSURIHandler;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.w3c.dom.Document;

abstract class AbstractAclWFSIntegrationTest extends WFSTestSupport {

    protected AclIntegrationTestSupport support;
    protected AclResourceAccessManager accessManager;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-test.xml");
    }

    /**
     * Avoids a several seconds (42 seconds in my case) startup penalty at {@link WFSXmlUtils#initRequestParser()} call
     * to {@link InetAddress#getHostName()} from:
     *
     * <pre>
     * WFSXmlUtils#initRequestParser()
     * -> WFSURIHandler()
     *   -> WFSURIHandler.initAddresses()
     *     -> InetAddress.getHostName()
     * </pre>
     */
    @BeforeClass
    public static void disableHostnameLookup() {
        System.setProperty(WFSURIHandler.class.getName() + ".disabled", "true");
    }

    @Before
    public void beforeEeach() {
        support = new AclIntegrationTestSupport(() -> GeoServerSystemTestSupport.applicationContext);
        support.before();
        accessManager = applicationContext.getBean(AclResourceAccessManager.class);
        // reset default config
        accessManager.initDefaults();
    }

    @After
    public void afterEach() {
        support.after();
    }

    protected Catalog getRawCatalog() {
        return support.getRawCatalog();
    }

    protected void assertExceptionReport(Document exceptionReport, String expectedValue) throws XpathException {
        assertXpathEvaluatesTo(expectedValue, "//ows:ExceptionReport/ows:Exception/ows:ExceptionText", exceptionReport);
    }
}
