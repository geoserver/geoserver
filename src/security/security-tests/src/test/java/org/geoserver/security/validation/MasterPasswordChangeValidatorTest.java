/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

import static org.junit.Assert.*;

import java.net.URL;
import org.geoserver.security.password.*;
import org.geoserver.security.password.URLMasterPasswordProvider.URLMasterPasswordProviderValidator;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Before;
import org.junit.Test;

public class MasterPasswordChangeValidatorTest extends GeoServerMockTestSupport {

    MasterPasswordChangeValidator validator;

    @Before
    public void setValidator() {
        validator = new MasterPasswordChangeValidator(getSecurityManager());
    }

    protected void checkCurrentPassword(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(ex, MasterPasswordChangeException.CURRENT_PASSWORD_REQUIRED);
        }
        r.setCurrentPassword("blabalb".toCharArray());
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(ex, MasterPasswordChangeException.CURRENT_PASSWORD_ERROR);
        }
    }

    protected void checkConfirmationPassword(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(
                    ex, MasterPasswordChangeException.CONFIRMATION_PASSWORD_REQUIRED);
        }
    }

    protected void checkNewPassword(MasterPasswordChangeRequest r) throws Exception {
        boolean fail = false;
        try {
            validator.validateChangeRequest(r);
        } catch (MasterPasswordChangeException ex) {
            fail = true;
            assertSecurityException(ex, MasterPasswordChangeException.NEW_PASSWORD_REQUIRED);
        }
        assertTrue(fail);
    }

    protected void checkConfirmationEqualsNewPassword(MasterPasswordChangeRequest r)
            throws Exception {
        boolean fail = false;
        try {
            validator.validateChangeRequest(r);
        } catch (MasterPasswordChangeException ex) {
            fail = true;
            assertSecurityException(
                    ex, MasterPasswordChangeException.PASSWORD_AND_CONFIRMATION_NOT_EQUAL);
        }
        assertTrue(fail);
    }

    protected void checkCurrentEqualsNewPassword(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (MasterPasswordChangeException ex) {
            assertSecurityException(ex, MasterPasswordChangeException.NEW_EQUALS_CURRENT);
        }
    }

    protected void validateAgainstPolicy(MasterPasswordChangeRequest r) throws Exception {
        try {
            validator.validateChangeRequest(r);
            fail();
        } catch (PasswordPolicyException ex) {
        }
    }

    @Test
    public void testUrlConfig() throws Exception {
        URLMasterPasswordProviderValidator validator =
                new URLMasterPasswordProviderValidator(getSecurityManager());

        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("foo");
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        try {
            validator.validateAddMasterPasswordProvider(config);
            // getSecurityManager().saveMasterPasswordProviderConfig(config);
            fail();
        } catch (URLMasterPasswordProviderException e) {
            assertSecurityException(e, URLMasterPasswordProviderException.URL_REQUIRED);
        }
        config.setURL(new URL("file:ABC"));
        config.setReadOnly(true);
        try {
            validator.validateAddMasterPasswordProvider(config);
            // getSecurityManager().saveMasterPasswordProviderConfig(config);
            fail();
        } catch (URLMasterPasswordProviderException e) {
            assertSecurityException(
                    e,
                    URLMasterPasswordProviderException.URL_LOCATION_NOT_READABLE,
                    new URL("file:ABC"));
        }
    }

    @Test
    public void testValidator() throws Exception {
        // test spring
        MasterPasswordChangeRequest r = new MasterPasswordChangeRequest();

        checkCurrentPassword(r);
        r.setCurrentPassword("geoserver".toCharArray());
        // r.setCurrentPassword(getMasterPassword().toCharArray());

        checkConfirmationPassword(r);
        r.setConfirmPassword("abc".toCharArray());

        checkNewPassword(r);
        r.setNewPassword("def".toCharArray());

        checkConfirmationEqualsNewPassword(r);
        r.setNewPassword("abc".toCharArray());

        validateAgainstPolicy(r);

        r.setConfirmPassword(r.getCurrentPassword());
        r.setNewPassword(r.getCurrentPassword());

        checkCurrentEqualsNewPassword(r);
        r.setConfirmPassword((new String(r.getCurrentPassword()) + "1").toCharArray());
        r.setNewPassword((new String(r.getCurrentPassword()) + "1").toCharArray());

        validator.validateChangeRequest(r);
    }

    protected void assertSecurityException(
            MasterPasswordChangeException ex, String id, Object... params) {

        assertEquals(id, ex.getId());
        assertEquals(params.length, ex.getArgs().length);
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], ex.getArgs()[i]);
        }
    }

    protected void assertSecurityException(
            MasterPasswordProviderException ex, String id, Object... params) {

        assertEquals(id, ex.getId());
        assertEquals(params.length, ex.getArgs().length);
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], ex.getArgs()[i]);
        }
    }
}
