package org.geoserver.security.validation;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Properties;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.AbstractSecurityServiceTest;
import org.geoserver.security.password.MasterPasswordChangeRequest;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.MasterPasswordProviderException;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.security.password.URLMasterPasswordProviderException;

public class MasterPasswordChangeValidatorTest extends AbstractSecurityServiceTest {

    MasterPasswordChangeValidator validator;
       
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
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
            assertSecurityException(ex, MasterPasswordChangeException.CONFIRMATION_PASSWORD_REQUIRED);
        }
    }
    
    protected void checkNewPassword(MasterPasswordChangeRequest r) throws Exception {
        boolean fail = false;
        try {
            validator.validateChangeRequest(r);
        } catch (MasterPasswordChangeException ex) {
            fail=true;
            assertSecurityException(ex, MasterPasswordChangeException.NEW_PASSWORD_REQUIRED);
        }
        assertTrue(fail);
    }

    protected void checkConfirmationEqualsNewPassword(MasterPasswordChangeRequest r) throws Exception {
        boolean fail = false;
        try {
            validator.validateChangeRequest(r);
        } catch (MasterPasswordChangeException ex) {
            fail=true;
            assertSecurityException(ex, 
                    MasterPasswordChangeException.PASSWORD_AND_CONFIRMATION_NOT_EQUAL);
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

    public void testUrlConfig() throws Exception {
        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("foo");
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        try {
            getSecurityManager().saveMasterPasswordProviderConfig(config);
            fail();
        }
        catch(URLMasterPasswordProviderException e) {
            assertSecurityException(e, URLMasterPasswordProviderException.URL_REQUIRED);
        }
        config.setURL(new URL("file:ABC"));
        config.setReadOnly(true);
        try {
            getSecurityManager().saveMasterPasswordProviderConfig(config);
            fail();
        }
        catch(URLMasterPasswordProviderException e) {
            assertSecurityException(e, 
                URLMasterPasswordProviderException.URL_LOCATION_NOT_READABLE, new URL("file:ABC"));
        }
    }

    public void testValidator() throws Exception{
        // test spring
        MasterPasswordChangeRequest r = new MasterPasswordChangeRequest();
        
        checkCurrentPassword(r);
        r.setCurrentPassword(GeoServerSecurityManager.MASTER_PASSWD_DEFAULT);
        
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
        r.setConfirmPassword((new String(r.getCurrentPassword())+"1").toCharArray());
        r.setNewPassword((new String(r.getCurrentPassword())+"1").toCharArray());

        validator.validateChangeRequest(r);
    }

    protected void assertSecurityException (MasterPasswordChangeException ex, String id, Object... params) {
        
        assertEquals(id,ex.getId());
        assertEquals(params.length, ex.getArgs().length);
        for (int i = 0; i <  params.length ;i++) {
            assertEquals(params[i], ex.getArgs()[i]);
        }
    }

    protected void assertSecurityException (MasterPasswordProviderException ex, String id, Object... params) {
        
        assertEquals(id,ex.getId());
        assertEquals(params.length, ex.getArgs().length);
        for (int i = 0; i <  params.length ;i++) {
            assertEquals(params[i], ex.getArgs()[i]);
        }
    }
}
