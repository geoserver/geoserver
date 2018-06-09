/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.*;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityManagerConfig;
import org.junit.Test;

public class SecuritySettingsPageTest extends AbstractSecurityWicketTestSupport {

    GeoServerSecurityManager manager;

    @Test
    public void testMangerConfigPanel() throws Exception {
        initializeForXML();
        createUserPasswordAuthProvider("default2", "default");
        activateRORoleService();
        manager = getSecurityManager();

        tester.startPage(new SecuritySettingsPage());
        tester.assertRenderedPage(SecuritySettingsPage.class);

        SecurityManagerConfig config = manager.getSecurityConfig();

        tester.assertModelValue("form:roleServiceName", "default");
        tester.assertModelValue(
                "form:encryption:encryptingUrlParams", config.isEncryptingUrlParams());
        tester.assertModelValue(
                "form:encryption:configPasswordEncrypterName", getPBEPasswordEncoder().getName());

        FormTester form = tester.newFormTester("form");

        form.setValue("roleServiceName", getRORoleServiceName());
        form.setValue("encryption:encryptingUrlParams", false);
        form.setValue(
                "encryption:configPasswordEncrypterName", getPlainTextPasswordEncoder().getName());
        form.submit("save");
        tester.assertNoErrorMessage();

        assertEquals(false, manager.getSecurityConfig().isEncryptingUrlParams());
        assertEquals(
                getPlainTextPasswordEncoder().getName(),
                manager.getSecurityConfig().getConfigPasswordEncrypterName());
        assertEquals(getRORoleServiceName(), manager.getActiveRoleService().getName());
    }
}
