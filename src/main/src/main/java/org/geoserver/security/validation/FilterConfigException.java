/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

/**
 * Exception for filter configurations
 * 
 * @author mcr
 *
 */
public class FilterConfigException extends SecurityConfigException {

    private static final long serialVersionUID = 1L;

    public FilterConfigException(String errorId, Object... args) {
        super(errorId, args);
        
    }

    public FilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
        
    }
    
    public static final String HEADER_ATTRIBUTE_NAME_REQUIRED="HEADER_ATTRIBUTE_NAME_REQUIRED";
    public static final String UNKNOWN_ROLE_CONVERTER="UNKNOWN_ROLE_CONVERTER";

    public static final String USER_GROUP_SERVICE_NEEDED="USER_GROUP_SERVICE_NEEDED";
    public static final String UNKNOWN_USER_GROUP_SERVICE="UNKNOWN_USER_GROUP_SERVICE";
    public static final String INVALID_SECONDS="INVALID_SECONDS";
    
    public static final String UNKNOWN_ROLE_SERVICE="UNKNOWN_ROLE_SERVICE";
    
    public static final String ROLE_SOURCE_NEEDED="ROLE_SOURCE_NEEDED";
    public static final String PASSWORD_PARAMETER_NAME_NEEDED="PASSWORD_PARAMETER_NAME_NEEDED";
    public static final String USER_PARAMETER_NAME_NEEDED="USER_PARAMETER_NAME_NEEDED";

    public static final String ACCESS_DENIED_PAGE_NEEDED="ACCESS_DENIED_PAGE_NEEDED";
    public static final String ACCESS_DENIED_PAGE_PREFIX="ACCESS_DENIED_PAGE_PREFIX";
    public static final String INVALID_ENTRY_POINT="INVALID_ENTRY_POINT";
    
    public static final String PRINCIPAL_HEADER_ATTRIBUTE_NEEDED="PRINCIPAL_HEADER_ATTRIBUTE_NEEDED";
    public static final String ROLES_HEADER_ATTRIBUTE_NEEDED="ROLES_HEADER_ATTRIBUTE_NEEDED";
    
    public static final String SECURITY_METADATA_SOURCE_NEEDED="SECURITY_METADATA_SOURCE_NEEDED";
    public static final String UNKNOWN_SECURITY_METADATA_SOURCE="UNKNOWN_SECURITY_METADATA_SOURCE";
    public static final String NO_AUTH_ENTRY_POINT="NO_AUTH_ENTRY_POINT";
    
    public static final String CAS_SERVICE_URL_REQUIRED ="CAS_SERVICE_URL_REQUIRED";
    public static final String CAS_SERVER_URL_REQUIRED ="CAS_SERVER_URL_REQUIRED";
    public static final String CAS_TICKETVALIDATOR_URL_REQUIRED ="CAS_TICKETVALIDATOR_URL_REQUIRED";
    public static final String CAS_SERVICE_URL_MALFORMED ="CAS_SERVICE_URL_MALFORMED";
    public static final String CAS_SERVER_URL_MALFORMED ="CAS_SERVER_URL_MALFORMED";
    public static final String CAS_TICKETVALIDATOR_URL_MALFORMED ="CAS_TICKETVALIDATOR_URL_MALFORMED";
    public static final String CAS_SERVICE_URL_SUFFIX ="CAS_SERVICE_URL_SUFFIX";

}
