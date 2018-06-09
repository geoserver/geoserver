/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import static org.junit.Assert.*;

import java.io.File;
import org.geoserver.data.test.Security_2_2_TestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.util.StringUtils;

/**
 * Tests migration from 2.2.x to 2.3.x
 *
 * @author mcr
 */
public class MigrateFrom_2_2_Test extends GeoServerSystemTestSupport {

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new Security_2_2_TestData();
    }

    @Test
    public void testMigration() throws Exception {

        File logoutFilterDir =
                new File(
                        getSecurityManager().get("security/filter").dir(),
                        GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        File oldLogoutFilterConfig = new File(logoutFilterDir, "config.xml.2.2.x");
        assertTrue(oldLogoutFilterConfig.exists());

        File oldSecManagerConfig =
                new File(getSecurityManager().get("security").dir(), "config.xml.2.2.x");
        assertTrue(oldSecManagerConfig.exists());

        RoleFilterConfig rfConfig =
                (RoleFilterConfig)
                        getSecurityManager()
                                .loadFilterConfig(GeoServerSecurityFilterChain.ROLE_FILTER);

        assertNotNull(rfConfig);

        SSLFilterConfig sslConfig =
                (SSLFilterConfig)
                        getSecurityManager()
                                .loadFilterConfig(GeoServerSecurityFilterChain.SSL_FILTER);

        assertNotNull(sslConfig);

        assertNull(
                getSecurityManager()
                        .loadFilterConfig(
                                GeoServerSecurityFilterChain.GUI_EXCEPTION_TRANSLATION_FILTER));

        SecurityManagerConfig config = getSecurityManager().loadSecurityConfig();
        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {
            assertFalse(
                    chain.getFilterNames()
                            .contains(
                                    GeoServerSecurityFilterChain
                                            .DYNAMIC_EXCEPTION_TRANSLATION_FILTER));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER));

            assertFalse(chain.isDisabled());
            assertFalse(chain.isRequireSSL());
            assertFalse(StringUtils.hasLength(chain.getRoleFilterName()));

            if (GeoServerSecurityFilterChain.WEB_CHAIN_NAME.equals(chain.getName())
                    || GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME.equals(chain.getName())
                    || GeoServerSecurityFilterChain.WEB_LOGOUT_CHAIN_NAME.equals(chain.getName()))
                assertTrue(chain.isAllowSessionCreation());
            else assertFalse(chain.isAllowSessionCreation());

            if (chain instanceof VariableFilterChain) {
                VariableFilterChain vchain = (VariableFilterChain) chain;

                assertEquals(
                        GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
                        vchain.getExceptionTranslationName());

                if (GeoServerSecurityFilterChain.REST_CHAIN_NAME.equals(vchain.getName())
                        || GeoServerSecurityFilterChain.GWC_CHAIN_NAME.equals(vchain.getName()))
                    assertEquals(
                            GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR,
                            vchain.getInterceptorName());
                else
                    assertEquals(
                            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR,
                            vchain.getInterceptorName());
            }
        }
    }

    @Test
    public void testWebLoginChainSessionCreation() throws Exception {
        // GEOS-6077
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        RequestFilterChain chain =
                config.getFilterChain()
                        .getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }
}
