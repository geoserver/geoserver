package org.geoserver.security.validation;

import static org.geoserver.security.validation.PasswordPolicyException.IS_NULL;
import static org.geoserver.security.validation.PasswordPolicyException.MAX_LENGTH_$1;
import static org.geoserver.security.validation.PasswordPolicyException.MIN_LENGTH_$1;
import static org.geoserver.security.validation.PasswordPolicyException.NO_DIGIT;
import static org.geoserver.security.validation.PasswordPolicyException.NO_LOWERCASE;
import static org.geoserver.security.validation.PasswordPolicyException.NO_UPPERCASE;
import static org.geoserver.security.validation.PasswordPolicyException.RESERVED_PREFIX_$1;

import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.impl.AbstractSecurityServiceTest;

public class PasswordValidatorTest extends AbstractSecurityServiceTest {

    PasswordPolicyConfig config;
    PasswordValidatorImpl validator;
       
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        config = new PasswordPolicyConfig();
        validator = new PasswordValidatorImpl(getSecurityManager());
        validator.setConfig(config);
        
    }
    
    public void testPasswords() throws PasswordPolicyException{
        checkForException(null, IS_NULL);
        
        validator.validatePassword("".toCharArray());
        validator.validatePassword("a".toCharArray());
        
        
        checkForException("plain:a", RESERVED_PREFIX_$1,"plain:");
        checkForException("crypt1:a", RESERVED_PREFIX_$1,"crypt1:");
        checkForException("digest1:a", RESERVED_PREFIX_$1,"digest1:");
        
        validator.validatePassword("plain".toCharArray());
        validator.validatePassword("plaina".toCharArray());
        
        config.setMinLength(2);
        checkForException("a", MIN_LENGTH_$1,2);
        validator.validatePassword("aa".toCharArray());
        
        config.setMaxLength(10);
        checkForException("01234567890", MAX_LENGTH_$1,10);
        validator.validatePassword("0123456789".toCharArray());
        
        config.setDigitRequired(true);
        checkForException("abcdef", NO_DIGIT);

        validator.validatePassword("abcde4".toCharArray());
        
        config.setUppercaseRequired(true);
        checkForException("abcdef4", NO_UPPERCASE);
        validator.validatePassword("abcde4F".toCharArray());
        
        config.setLowercaseRequired(true);
        checkForException("ABCDE4F", NO_LOWERCASE);
        validator.validatePassword("abcde4F".toCharArray());        
    }
    
    
        
    protected void checkForException(String password, String id,Object... params) {
        try {
            validator.validatePassword(password != null ? password.toCharArray() : null);
        } catch (PasswordPolicyException ex) {
            assertEquals(id,ex.getId());
            assertEquals(params.length, ex.getArgs().length);
            for (int i = 0; i <  params.length ;i++) {
                assertEquals(params[i], ex.getArgs()[i]);
            }
        }        
    }
}
