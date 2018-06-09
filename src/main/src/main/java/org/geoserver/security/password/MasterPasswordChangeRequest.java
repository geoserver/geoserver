/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

/**
 * Model object for a master password change request.
 *
 * @author mcr
 */
public class MasterPasswordChangeRequest {
    private char[] currentPassword;
    private char[] newPassword;
    private char[] confirmPassword;

    public char[] getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(char[] currentPassword) {
        this.currentPassword = currentPassword;
    }

    public char[] getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(char[] newPassword) {
        this.newPassword = newPassword;
    }

    public char[] getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(char[] confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
