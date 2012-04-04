/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.validation;

import java.text.MessageFormat;

/**
 * Exception used for validation errors 
 * during security configuration
 * 
 * @author christian
 *
 */
public class SecurityConfigException extends AbstractSecurityException {
    private static final long serialVersionUID = 1L;

    public static final String INVALID_PASSWORD_ENCODER_$1 = "INVALID_PASSWORD_ENCODER";
    //return MessageFormat.format("Bean {0} is not a valid configuration password encoder",args);
    @Deprecated
    public static final String SEC_ERR_01 = INVALID_PASSWORD_ENCODER_$1;

    public static final String ROLE_SERVICE_NOT_FOUND_$1 = "ROLE_SERVICE_NOT_FOUND";
    //return MessageFormat.format("No role service named {0} ",args);
    @Deprecated
    public static final String SEC_ERR_02 = ROLE_SERVICE_NOT_FOUND_$1;

    public static final String AUTH_PROVIDER_NOT_FOUND_$1 = "AUTH_PROVIDER_NOT_FOUND";
    //return MessageFormat.format("No authentication provider named {0} ",args);
    @Deprecated
    public static final String SEC_ERR_03 = AUTH_PROVIDER_NOT_FOUND_$1;

    public static final String INVALID_CONFIG_PASSWORD_ENCODER_$1 = "INVALID_CONFIG_PASSWORD_ENCODER";
    //return MessageFormat.format("Bean {0} is not a valid password password encoder",args);
    @Deprecated
    public static final String SEC_ERR_04 = INVALID_CONFIG_PASSWORD_ENCODER_$1;

    public static final String INVALID_STRONG_CONFIG_PASSWORD_ENCODER = "INVALID_STRONG_CONFIG_PASSWORD_ENCODER";
    //return MessageFormat.format("Install unrestricted security policy files before using a strong configuration password encoder",args);
    @Deprecated
    public static final String SEC_ERR_05 = INVALID_STRONG_CONFIG_PASSWORD_ENCODER;

    public static final String INVALID_STRONG_PASSWORD_ENCODER = "INVALID_CONFIG_PASSWORD_ENCODER";
    //return MessageFormat.format("Install unrestricted security policy files before using a strong user password encoder",args);
    @Deprecated
    public static final String SEC_ERR_06 = INVALID_STRONG_PASSWORD_ENCODER;

    public static final String PASSWORD_ENCODER_REQUIRED = "PASSWORD_ENCODER_REQUIRED";
    //return MessageFormat.format("Configuration password encoder is required",args);
    @Deprecated
    public static final String SEC_ERR_07 = PASSWORD_ENCODER_REQUIRED;

    public static final String CLASS_NOT_FOUND_$1 = "CLASS_NOT_FOUND";
    //return MessageFormat.format("Class not found {0} ",args);
    @Deprecated
    public static final String SEC_ERR_20 = CLASS_NOT_FOUND_$1;

    public static final String CLASS_WRONG_TYPE_$2 = "CLASS_WRONG_TYPE";
    //return MessageFormat.format("Class {1} ist not of type {0} ",args);
    @Deprecated
    public static final String SEC_ERR_21 = CLASS_WRONG_TYPE_$2;

    public static final String NAME_REQUIRED = "NAME_REQUIRED";
    //return MessageFormat.format("Name is required ",args);
    @Deprecated
    public static final String SEC_ERR_22 = NAME_REQUIRED;

    public static final String AUTH_PROVIDER_ALREADY_EXISTS_$1 = "AUTH_PROVIDER_ALREADY_EXISTS";
    //return MessageFormat.format("Authentication provider {0} alreday exists",args);
    @Deprecated
    public static final String SEC_ERR_23a = AUTH_PROVIDER_ALREADY_EXISTS_$1;

    public static final String PASSWD_POLICY_ALREADY_EXISTS_$1 = "PASSWD_POLICY_ALREADY_EXISTS";
    //return MessageFormat.format("Password policy {0} alreday exists",args);
    @Deprecated
    public static final String SEC_ERR_23b = PASSWD_POLICY_ALREADY_EXISTS_$1;

    public static final String ROLE_SERVICE_ALREADY_EXISTS_$1 = "ROLE_SERVICE_ALREADY_EXISTS";
    //return MessageFormat.format("Role service {0} alreday exists",args);
    @Deprecated
    public static final String SEC_ERR_23c = ROLE_SERVICE_ALREADY_EXISTS_$1;

    public static final String USERGROUP_SERVICE_ALREADY_EXISTS_$1 = "USERGROUP_SERVICE_ALREADY_EXISTS";
    //return MessageFormat.format("User/group service {0} alreday exists",args);
    @Deprecated
    public static final String SEC_ERR_23d = USERGROUP_SERVICE_ALREADY_EXISTS_$1;

    public static final String AUTH_FILTER_ALREADY_EXISTS_$1 = "AUTH_FILTER_ALREADY_EXISTS";
    //return MessageFormat.format("Authentication filter {0} alreday exists",args);
    @Deprecated
    public static final String SEC_ERR_23e = AUTH_FILTER_ALREADY_EXISTS_$1;
    
    @Deprecated
    public static final String SEC_ERR_24a = AUTH_PROVIDER_NOT_FOUND_$1;

    public static final String PASSWD_POLICY_NOT_FOUND_$1 = "PASSWD_POLICY_NOT_FOUND";
    //return MessageFormat.format("Password policy {0} does not exist",args);
    @Deprecated
    public static final String SEC_ERR_24b = PASSWD_POLICY_NOT_FOUND_$1;

    @Deprecated
    public static final String SEC_ERR_24c = ROLE_SERVICE_NOT_FOUND_$1;
    
    public static final String USERGROUP_SERVICE_NOT_FOUND_$1 = "USERGROUP_SERVICE_NOT_FOUND";
    //return MessageFormat.format("User/group service {0} does not exist",args);
    @Deprecated
    public static final String SEC_ERR_24d = USERGROUP_SERVICE_NOT_FOUND_$1;
    
    public static final String AUTH_FILTER_NOT_FOUND_$1 = "AUTH_FILTER_NOT_FOUND";
    //return MessageFormat.format("Authentication filter {0} does not exist",args);
    @Deprecated
    public static final String SEC_ERR_24e = AUTH_FILTER_NOT_FOUND_$1;

    public static final String CLASSNAME_REQUIRED = "CLASSNAME_REQUIRED";
    //return MessageFormat.format("Implementation name is required",args);
    @Deprecated
    public static final String SEC_ERR_25 = CLASSNAME_REQUIRED;

    public static final String ROLE_SERVICE_ACTIVE_$1 = "ROLE_SERVICE_ACTIVE";
    //return MessageFormat.format("Role service {0} is active and cannot be deleted",args);
    @Deprecated
    public static final String SEC_ERR_30 = ROLE_SERVICE_ACTIVE_$1;
    
    public static final String AUTH_PROVIDER_ACTIVE_$1 = "AUTH_PROVIDER_ACTIVE";
    //return MessageFormat.format("Authentication provider {0} is active and cannot be deleted",args);
    @Deprecated
    public static final String SEC_ERR_31 = ROLE_SERVICE_ACTIVE_$1;
    
    public static final String PASSWD_ENCODER_REQUIRED_$1 = "PASSWD_ENCODER_REQUIRED";
    //return MessageFormat.format("No password encoder specified for user/group service {0}",args);
    @Deprecated
    public static final String SEC_ERR_32 = PASSWD_ENCODER_REQUIRED_$1;

    public static final String PASSWD_POLICY_REQUIRED_$1 = "PASSWD_POLICY_REQUIRED";
    //return MessageFormat.format("No password policy specified for user/group service {0}",args);
    @Deprecated
    public static final String SEC_ERR_33 = PASSWD_ENCODER_REQUIRED_$1;

    public static final String PASSWD_POLICY_ACTIVE_$2 = "PASSWD_POLICY_ACTIVE";
    //return MessageFormat.format("Password policy {0} is used by user/group service {1}",args);
    @Deprecated
    public static final String SEC_ERR_34 = PASSWD_POLICY_ACTIVE_$2;

    public static final String USERGROUP_SERVICE_ACTIVE_$2 = "USERGROUP_SERVICE_ACTIVE";
    //return MessageFormat.format("User/group service {0} is used by authentication provider {1}",args);
    @Deprecated
    public static final String SEC_ERR_35 = USERGROUP_SERVICE_ACTIVE_$2;

    public static final String INVALID_MIN_LENGTH = "INVALID_MIN_LENGTH";
    //return MessageFormat.format("Minimum length of password must be >= 0",args);
    @Deprecated
    public static final String SEC_ERR_40 = INVALID_MIN_LENGTH;

    public static final String INVALID_MAX_LENGTH = "INVALID_MAX_LENGTH";
    //return MessageFormat.format("Maximum length of password must be greater or equal to the minimum length",args);
    @Deprecated
    public static final String SEC_ERR_41 = INVALID_MAX_LENGTH;

    public static final String PASSWD_POLICY_MASTER_DELETE = "PASSWD_POLICY_MASTER_DELETE";
    //return MessageFormat.format("Policy for the master password cannot be deleted",args);
    @Deprecated
    public static final String SEC_ERR_42 = PASSWD_POLICY_MASTER_DELETE;

    
    public static final String FILTER_CHAIN_CONFIG_ERROR="FILTER_CHAIN_CONFIG_ERROR";
    
    public static final String FILTER_STILL_USED="FILTER_STILL_USED";
    
    public SecurityConfigException(String errorId, Object[] args) {
        super(errorId, args);
    }

    public SecurityConfigException(String errorId, String message, Object[] args) {
        super(errorId, message, args);
    }
}
