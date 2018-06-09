/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

import org.geoserver.security.password.MasterPasswordChangeRequest;

/**
 * Exception used for validation errors concerning {@link MasterPasswordChangeRequest}
 *
 * @author christian
 */
public class MasterPasswordChangeException extends AbstractSecurityException {
    private static final long serialVersionUID = 1L;

    public static final String CURRENT_PASSWORD_REQUIRED = "CURRENT_PASSWORD_REQUIRED";
    public static final String CURRENT_PASSWORD_ERROR = "CURRENT_PASSWORD_ERROR";
    public static final String CONFIRMATION_PASSWORD_REQUIRED = "CONFIRMATION_PASSWORD_REQUIRED";
    public static final String PASSWORD_AND_CONFIRMATION_NOT_EQUAL =
            "PASSWORD_AND_CONFIRMATION_NOT_EQUAL";
    public static final String NEW_PASSWORD_REQUIRED = "NEW_PASSWORD_REQUIRED";
    public static final String NEW_EQUALS_CURRENT = "NEW_EQUALS_CURRENT";

    public MasterPasswordChangeException(String errorId, Object[] args) {
        super(errorId, args);
    }

    public MasterPasswordChangeException(String errorId, String message, Object[] args) {
        super(errorId, message, args);
    }
}
