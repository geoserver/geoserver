/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.validation;

public class PasswordPolicyException extends AbstractSecurityException {
    private static final long serialVersionUID = 1L;

    public final static String IS_NULL="IS_NULL";
    //return MessageFormat.format("Password is mandatory",args);

    @Deprecated
    public final static String PW_IS_NULL = IS_NULL;
    
    public final static String NO_DIGIT="NO_DIGIT";
    //return MessageFormat.format("Password does not contain a digit",args);
    
    @Deprecated
    public final static String PW_NO_DIGIT = NO_DIGIT;
    
    public final static String NO_UPPERCASE="NO_UPPERCASE";
    //return MessageFormat.format("password does not contain an upper case letter",args);
    
    @Deprecated
    public final static String PW_NO_UPPERCASE = NO_UPPERCASE;
    
    public final static String NO_LOWERCASE="NO_LOWERCASE";
    //return MessageFormat.format("password does not contain a lower case letter",args);
    
    @Deprecated
    public final static String PW_NO_LOWERCASE = NO_LOWERCASE;

    public final static String MIN_LENGTH_$1="MIN_LENGTH";
    //return MessageFormat.format("password must have {0} characters",args);
    
    @Deprecated
    public final static String PW_MIN_LENGTH = MIN_LENGTH_$1;
    
    public final static String MAX_LENGTH_$1="MAX_LENGTH";
    //return MessageFormat.format("password has more than {0} characters",args);
    
    @Deprecated
    public final static String PW_MAX_LENGTH = MAX_LENGTH_$1;
    
    public final static String RESERVED_PREFIX_$1="RESERVED_PREFIX";
    //return MessageFormat.format("password  starts with reserved prefix {0}",args);
    
    @Deprecated
    public final static String PW_RESERVED_PREFIX = RESERVED_PREFIX_$1;

    public PasswordPolicyException(String errorId, Object[] args) {
        super(errorId, args);
    }

    public PasswordPolicyException(String errorId, String message, Object[] args) {
        super(errorId, message, args);
    } 
}
