package org.geoserver.security.web.passwd;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.GeoServerWicketTestSupport;

public class MasterPasswordChangePanelTest extends AbstractSecurityWicketTestSupport {

    FormTester ft;
    @Override
    protected void setUpInternal() throws Exception {
        tester.startPage(new MasterPasswordChangePage());
        tester.assertRenderedPage(MasterPasswordChangePage.class);

        ft = tester.newFormTester("form");
    }
    
    public void testRequiredFields() throws Exception {
        ft.submit();
        tester.assertErrorMessages(new String[]{"Field 'Current password' is required.", 
            "Field 'New password' is required.", "Field 'Confirmation' is required." });
    }
    
    public void testBadCurrentPassword() throws Exception {
        ft.setValue("currentPassword", "foo");
        ft.setValue("newPassword", "bar");
        ft.setValue("newPasswordConfirm", "bar");
        ft.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*Current master password invalid.*"));
    }
    
    public void testPasswordViolatesPolicy() throws Exception {
        ft.setValue("currentPassword", "geoserver");
        ft.setValue("newPassword", "bar");
        ft.setValue("newPasswordConfirm", "bar");
        ft.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*PasswordPolicyException.*"));
    }
    
    public void testPasswordChange() throws Exception {
        ft.setValue("currentPassword", "geoserver");
        ft.setValue("newPassword", "Foobar2012");
        ft.setValue("newPasswordConfirm", "Foobar2012");
        ft.submit("save");
        tester.assertNoErrorMessage();
        assertTrue(getSecurityManager().checkMasterPassword("Foobar2012"));
    }
}
