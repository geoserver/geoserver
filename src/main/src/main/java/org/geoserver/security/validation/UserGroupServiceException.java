/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.validation;

import java.text.MessageFormat;

import org.geoserver.security.GeoServerUserGroupService;

/**
 * Exception used for validation errors  
 * concerning {@link GeoServerUserGroupService}
 * 
 * @author christian
 *
 */
public class UserGroupServiceException extends AbstractSecurityException {
    private static final long serialVersionUID = 1L;

    public static final String USERNAME_REQUIRED = "USERNAME_REQUIRED";
    //return MessageFormat.format("User name is mandatory",args);

    @Deprecated
    public static final String UG_ERR_01 = USERNAME_REQUIRED;

    public static final String GROUPNAME_REQUIRED = "GROUPNAME_REQUIRED";
    //return MessageFormat.format("Group name is mandatory",args);
    
    @Deprecated
    public static final String UG_ERR_02 = GROUPNAME_REQUIRED;

    public static final String USER_NOT_FOUND_$1 = "USER_NOT_FOUND";
    //return MessageFormat.format("User {0} does not exist",args);

    @Deprecated
    public static final String UG_ERR_03 = USER_NOT_FOUND_$1;
    
    public static final String GROUP_NOT_FOUND_$1 = "GROUP_NOT_FOUND";
    //return MessageFormat.format("Group {0} does not exist",args)

    @Deprecated
    public static final String UG_ERR_04 = GROUP_NOT_FOUND_$1;

    public static final String USER_ALREADY_EXISTS_$1 = "USER_ALREADY_EXISTS";
    //return MessageFormat.format("User {0} already exists",args);

    @Deprecated
    public static final String UG_ERR_05 = USER_ALREADY_EXISTS_$1;

    public static final String GROUP_ALREADY_EXISTS_$1 = "GROUP_ALREADY_EXISTS";
    //return MessageFormat.format("Group {0} already exists",args);
    
    @Deprecated
    public static final String UG_ERR_06 = GROUP_ALREADY_EXISTS_$1;

    public static final String USER_IN_OTHER_GROUP_NOT_MODIFIABLE_$1 = "USER_IN_OTHER_GROUP_NOT_MODIFIABLE";

    public UserGroupServiceException(String errorId, Object[] args) {
        super(errorId, args);
    }

    public UserGroupServiceException(String errorId, String message, Object[] args) {
        super(errorId, message, args);
    }
}
