/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import static org.junit.Assert.assertTrue;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class MasterPasswordChangePanelTest extends AbstractSecurityWicketTestSupport {

    FormTester ft;

    @Before
    public void setUp() throws Exception {
        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);

        login();
        tester.startPage(new MasterPasswordChangePage());
        tester.assertRenderedPage(MasterPasswordChangePage.class);

        ft = tester.newFormTester("form");
    }

    @Test
    public void testRequiredFields() throws Exception {
        ft.submit();
        tester.assertErrorMessages(
                new String[] {
                    "Field 'Current password' is required.",
                    "Field 'New password' is required.",
                    "Field 'Confirmation' is required."
                });
    }

    @Test
    public void testBadCurrentPassword() throws Exception {
        ft.setValue("currentPassword", "foo");
        ft.setValue("newPassword", "bar");
        ft.setValue("newPasswordConfirm", "bar");
        ft.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*Current master password invalid.*"));
    }

    @Test
    public void testPasswordViolatesPolicy() throws Exception {
        String mpw = getMasterPassword();
        // System.out.println("testPasswordViolatesPolicy: " + mpw);
        ft.setValue("currentPassword", mpw);
        ft.setValue("newPassword", "bar");
        ft.setValue("newPasswordConfirm", "bar");
        ft.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*PasswordPolicyException.*"));
    }

    @Test
    public void testPasswordChange() throws Exception {
        String mpw = getMasterPassword();
        // System.out.println("testPasswordChange: " + mpw);
        ft.setValue("currentPassword", mpw);
        ft.setValue("newPassword", "Foobar2012");
        ft.setValue("newPasswordConfirm", "Foobar2012");
        ft.submit("save");
        tester.assertNoErrorMessage();
        assertTrue(getSecurityManager().checkMasterPassword("Foobar2012"));
    }
}
