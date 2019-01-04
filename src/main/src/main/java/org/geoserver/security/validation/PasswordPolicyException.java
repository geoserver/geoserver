/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

public class PasswordPolicyException extends AbstractSecurityException {
    private static final long serialVersionUID = 1L;

    public static final String IS_NULL = "IS_NULL";
    // return MessageFormat.format("Password is mandatory",args);

    public static final String NO_DIGIT = "NO_DIGIT";
    // return MessageFormat.format("Password does not contain a digit",args);

    public static final String NO_UPPERCASE = "NO_UPPERCASE";
    // return MessageFormat.format("password does not contain an upper case letter",args);

    public static final String NO_LOWERCASE = "NO_LOWERCASE";
    // return MessageFormat.format("password does not contain a lower case letter",args);

    public static final String MIN_LENGTH_$1 = "MIN_LENGTH";
    // return MessageFormat.format("password must have {0} characters",args);

    public static final String MAX_LENGTH_$1 = "MAX_LENGTH";
    // return MessageFormat.format("password has more than {0} characters",args);

    public static final String RESERVED_PREFIX_$1 = "RESERVED_PREFIX";
    // return MessageFormat.format("password  starts with reserved prefix {0}",args);

    public PasswordPolicyException(String errorId, Object[] args) {
        super(errorId, args);
    }

    public PasswordPolicyException(String errorId, String message, Object[] args) {
        super(errorId, message, args);
    }
}
