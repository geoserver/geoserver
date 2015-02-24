/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.geoxacml;

import org.geoserver.security.DataAccessManager;
import org.geoserver.security.SecureCatalogImplTest;
import org.geoserver.xacml.role.XACMLRole;
import org.geoserver.xacml.security.XACMLDataAccessManager;
import org.springframework.security.providers.TestingAuthenticationToken;

public class XACMLSecureCatalogImplTest extends SecureCatalogImplTest {

    @Override
    protected DataAccessManager buildManager(String propertyFile) throws Exception {

        if ("wideOpen.properties".equals(propertyFile)) {
            GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/wideOpen/");
        }
        if ("publicRead.properties".equals(propertyFile)) {
            GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/publicRead/");
        }
        if ("lockedDownMixed.properties".equals(propertyFile)) {
            GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/lockedDownMixed/");
        }
        if ("lockedDownChallenge.properties".equals(propertyFile)) {
            GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/lockedDownChallenge/");
        }
        if ("lockedDown.properties".equals(propertyFile)) {
            GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/lockedDown/");
        }
        if ("complex.properties".equals(propertyFile)) {
            GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/complex/");
        }

        GeoXACMLConfig.reset();
        return new XACMLDataAccessManager();

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rwUser = new TestingAuthenticationToken("rw", "supersecret", new XACMLRole[] {
                new XACMLRole("READER"), new XACMLRole("WRITER") });
        roUser = new TestingAuthenticationToken("ro", "supersecret",
                new XACMLRole[] { new XACMLRole("READER") });
        anonymous = new TestingAuthenticationToken("anonymous", "",
                new XACMLRole[] { new XACMLRole(XACMLConstants.AnonymousRole) });
        milUser = new TestingAuthenticationToken("military", "supersecret",
                new XACMLRole[] { new XACMLRole("MILITARY") });
        root = new TestingAuthenticationToken("admin", "geoserver",
                new XACMLRole[] { new XACMLRole(XACMLConstants.AdminRole) });

    }

}
