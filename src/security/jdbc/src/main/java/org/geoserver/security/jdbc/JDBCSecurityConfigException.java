package org.geoserver.security.jdbc;

import java.text.MessageFormat;

import org.geoserver.security.validation.SecurityConfigException;

public class JDBCSecurityConfigException extends SecurityConfigException {

    public static final String DRIVER_CLASSNAME_REQUIRED = "DRIVER_CLASSNAME_REQUIRED";
    //return MessageFormat.format("Driver name is mandatory",args);
    @Deprecated
    public static final String SEC_ERR_200 = DRIVER_CLASSNAME_REQUIRED; 
    
    public static final String USERNAME_REQUIRED = "USERNAME_REQUIRED";
    //return MessageFormat.format("Username is mandatory",args);
    @Deprecated
    public static final String SEC_ERR_201 = USERNAME_REQUIRED;

    public static final String JDBCURL_REQUIRED = "JDBCURL_REQUIRED";
    //return MessageFormat.format("Jdbc connect url is mandatory",args);
    @Deprecated
    public static final String SEC_ERR_202 = JDBCURL_REQUIRED;

    public static final String DRIVER_CLASS_NOT_FOUND_$1 = "DRIVER_CLASS_NOTFOUND";
    //return MessageFormat.format("Driver named {0} is not in class path",args);
    @Deprecated
    public static final String SEC_ERR_203 = DRIVER_CLASS_NOT_FOUND_$1;

    public static final String DDL_FILE_REQUIRED = "DDL_FILE_REQUIRED";
    //return MessageFormat.format("Cannot create tables without a DDL property file",args);
    @Deprecated
    public static final String SEC_ERR_204 = DDL_FILE_REQUIRED;

    public static final String JNDINAME_REQUIRED = "DDL_FILE_REQUIRED";
    //return MessageFormat.format("JNDI name is mandatory",args);
    @Deprecated
    public static final String SEC_ERR_210 = JNDINAME_REQUIRED;

    public static final String DDL_FILE_INVALID = "DDL_FILE_INVALID";
    //return MessageFormat.format("Cannot open DDL file {0}",args);
    @Deprecated
    public static final String SEC_ERR_211 = DDL_FILE_INVALID;

    public static final String DML_FILE_REQUIRED = "DML_FILE_REQUIRED";
    //return MessageFormat.format("DML file is required",args);
    @Deprecated
    public static final String SEC_ERR_212 = DML_FILE_REQUIRED;

    public static final String DML_FILE_INVALID = "DML_FILE_INVALID";
    //return MessageFormat.format("Cannot open DML file {0}",args);
    @Deprecated
    public static final String SEC_ERR_213 = DDL_FILE_INVALID;

    public JDBCSecurityConfigException(String errorId, Object[] args) {
        super(errorId, args);
    }
}
