/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import java.io.IOException;
import java.util.Arrays;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.MasterPasswordChangeRequest;
import org.geoserver.security.password.PasswordValidator;

/**
 * Validates a master password change request
 *
 * @author mcr
 */
public class MasterPasswordChangeValidator extends AbstractSecurityValidator {

    public MasterPasswordChangeValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    protected void checkCurrentPassword(MasterPasswordChangeRequest request)
            throws MasterPasswordChangeException {
        if (isNotEmpty(request.getCurrentPassword()) == false) {
            throw createSecurityException(MasterPasswordChangeException.CURRENT_PASSWORD_REQUIRED);
        }
        try {
            if (!manager.getKeyStoreProvider().isKeyStorePassword(request.getCurrentPassword())) {
                throw createSecurityException(MasterPasswordChangeException.CURRENT_PASSWORD_ERROR);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void checkConfirmationPassword(MasterPasswordChangeRequest request)
            throws MasterPasswordChangeException {
        if (isNotEmpty(request.getConfirmPassword()) == false) {
            throw createSecurityException(
                    MasterPasswordChangeException.CONFIRMATION_PASSWORD_REQUIRED);
        }
    }

    protected void checkNewPassword(MasterPasswordChangeRequest request)
            throws MasterPasswordChangeException {
        if (isNotEmpty(request.getNewPassword()) == false) {
            throw createSecurityException(MasterPasswordChangeException.NEW_PASSWORD_REQUIRED);
        }
    }

    protected void checkNewEqualsConfirmation(char[] newPassword, char[] confirmationPassword)
            throws MasterPasswordChangeException {
        if (!Arrays.equals(newPassword, confirmationPassword)) {
            throw createSecurityException(
                    MasterPasswordChangeException.PASSWORD_AND_CONFIRMATION_NOT_EQUAL);
        }
    }

    protected void checkNewEqualsCurrent(char[] newPassword, char[] currentPassword)
            throws MasterPasswordChangeException {
        if (Arrays.equals(newPassword, currentPassword)) {
            throw createSecurityException(MasterPasswordChangeException.NEW_EQUALS_CURRENT);
        }
    }

    /** Checks the {@link MasterPasswordChangeRequest} object */
    public void validateChangeRequest(MasterPasswordChangeRequest request)
            throws MasterPasswordChangeException, PasswordPolicyException {

        checkCurrentPassword(request);
        checkConfirmationPassword(request);
        checkNewPassword(request);
        checkNewEqualsConfirmation(request.getNewPassword(), request.getConfirmPassword());
        validatePasswordAgainstPolicy(request.getNewPassword());
        checkNewEqualsCurrent(request.getNewPassword(), request.getCurrentPassword());
    }

    /** Helper method for creating a proper {@link MasterPasswordChangeException} object */
    protected MasterPasswordChangeException createSecurityException(
            String errorid, Object... args) {
        return new MasterPasswordChangeException(errorid, args);
    }

    protected void validatePasswordAgainstPolicy(char[] password) throws PasswordPolicyException {
        PasswordValidator val = null;
        try {
            val = manager.loadPasswordValidator(PasswordValidatorImpl.MASTERPASSWORD_NAME);
            val.validatePassword(password);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
