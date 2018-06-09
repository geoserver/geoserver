/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import org.geoserver.security.validation.SecurityConfigException;

public class JDBCSecurityConfigException extends SecurityConfigException {

    private static final long serialVersionUID = 1L;

    public static final String DRIVER_CLASSNAME_REQUIRED = "DRIVER_CLASSNAME_REQUIRED";
    // return MessageFormat.format("Driver name is mandatory",args);

    public static final String USERNAME_REQUIRED = "USERNAME_REQUIRED";
    // return MessageFormat.format("Username is mandatory",args);

    public static final String JDBCURL_REQUIRED = "JDBCURL_REQUIRED";
    // return MessageFormat.format("Jdbc connect url is mandatory",args);

    public static final String DRIVER_CLASS_NOT_FOUND_$1 = "DRIVER_CLASS_NOTFOUND";
    // return MessageFormat.format("Driver named {0} is not in class path",args);

    public static final String DDL_FILE_REQUIRED = "DDL_FILE_REQUIRED";
    // return MessageFormat.format("Cannot create tables without a DDL property file",args);

    public static final String JNDINAME_REQUIRED = "JNDINAME_REQUIRED";
    // return MessageFormat.format("JNDI name is mandatory",args);

    public static final String DDL_FILE_INVALID = "DDL_FILE_INVALID";
    // return MessageFormat.format("Cannot open DDL file {0}",args);

    public static final String DML_FILE_REQUIRED = "DML_FILE_REQUIRED";
    // return MessageFormat.format("DML file is required",args);

    public static final String DML_FILE_INVALID = "DML_FILE_INVALID";
    // return MessageFormat.format("Cannot open DML file {0}",args);

    public JDBCSecurityConfigException(String errorId, Object[] args) {
        super(errorId, args);
    }
}
