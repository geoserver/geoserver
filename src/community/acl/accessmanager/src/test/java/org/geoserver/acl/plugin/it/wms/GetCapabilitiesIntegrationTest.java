/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.it.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.DENY;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.w3c.dom.Document;

public class GetCapabilitiesIntegrationTest extends AbstractAclWMSIntegrationTest {

    @Before
    public void setUpAcl() {
        // anonymous can only see cdf:* layers
        support.addRule(0, ALLOW, null, "ROLE_ANONYMOUS", null, null, "cdf", null);
        support.addRule(1, ALLOW, null, "ROLE_CITE", null, null, "cite", null);
        support.addRule(10, DENY, null, null, null, null, null, null);
    }

    void loginAsCite() {
        this.username = "cite";
        this.password = "cite";
        login("cite", "cite", "ROLE_CITE");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF");
    }

    @Override
    protected Authentication loginAsAdmin() {
        this.username = "admin";
        this.password = "geoserver";
        return super.loginAsAdmin();
    }

    @Test
    public void testAdmin() throws Exception {
        loginAsAdmin();

        // check from the caps he can access everything
        Document dom = getCapabilities();

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("8", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCite() throws Exception {
        loginAsCite();
        Document dom = getCapabilities();

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testAnnonymous() throws Exception {
        login("anonymousUser", "", "ROLE_ANONYMOUS");
        Document dom = getCapabilities();

        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("8", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    private Document getCapabilities() throws Exception {
        String path = "wms?request=GetCapabilities&version=1.1.1&service=WMS";
        return getAsDOM(path);
    }
}
