/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.xml;

import org.geoserver.security.validation.SecurityConfigException;

public class XMLSecurityConfigException extends SecurityConfigException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String CHECK_INTERVAL_INVALID = "CHECK_INTERVAL_INVALID";
    //return MessageFormat.format("Check interval in milliseconds can be 0 (disabled) or >= 1000",args);
    @Deprecated
    public static final String SEC_ERR_100 = CHECK_INTERVAL_INVALID;
    
    public static final String FILE_CREATE_FAILED_$1 = "FILE_CREATE_FAILED";
    //return MessageFormat.format("Cannot create file {0}",args);
    @Deprecated
    public static final String SEC_ERR_101 = FILE_CREATE_FAILED_$1;

    public static final String ROLE_SERVICE_NOT_EMPTY_$1 = "ROLE_SERVICE_NOT_EMPTY";
    //return MessageFormat.format("Role service {0} must be empty",args);
    @Deprecated
    public static final String SEC_ERR_102 = ROLE_SERVICE_NOT_EMPTY_$1;

    public static final String USERGROUP_SERVICE_NOT_EMPTY_$1 = "USERGROUP_SERVICE_NOT_EMPTY";
    //return MessageFormat.format("User/group service {0} must be empty",args);
    @Deprecated
    public static final String SEC_ERR_103 = USERGROUP_SERVICE_NOT_EMPTY_$1;
    
    public static final String FILENAME_REQUIRED = "FILENAME_REQUIRED";
    //return MessageFormat.format("File name required",args);
    @Deprecated
    public static final String SEC_ERR_104 = FILENAME_REQUIRED;

    public static final String FILENAME_CHANGE_INVALID_$2 = "FILENAME_CHANGE_INVALID";
    //return MessageFormat.format("Cannot change file name from {0} to {1}",args);
    @Deprecated
    public static final String SEC_ERR_105 = FILENAME_CHANGE_INVALID_$2;

    public static final String USERGROUP_SERVICE_REQUIRED = "USERGROUP_SERVICE_REQUIRED"; 
    //return MessageFormat.format("User/group service is required",args);
    @Deprecated
    public static final String SEC_ERR_106 = USERGROUP_SERVICE_REQUIRED;

    public XMLSecurityConfigException(String errorId, Object[] args) {
        super(errorId, args);
    }
}
