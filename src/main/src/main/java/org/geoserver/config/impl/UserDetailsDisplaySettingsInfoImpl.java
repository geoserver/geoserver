/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.config.UserDetailsDisplaySettingsInfo;

public class UserDetailsDisplaySettingsInfoImpl implements UserDetailsDisplaySettingsInfo {

    private String id;

    private LoggedInUserDisplayMode loggedInUserDisplayMode = LoggedInUserDisplayMode.USERNAME;
    private boolean showProfileColumnsInUserList = false;
    private EmailDisplayMode emailDisplayMode = EmailDisplayMode.DOMAIN_ONLY;
    private boolean revealEmailAtClick = false;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public LoggedInUserDisplayMode getLoggedInUserDisplayMode() {
        if (loggedInUserDisplayMode == null) {
            loggedInUserDisplayMode = LoggedInUserDisplayMode.USERNAME;
        }
        return loggedInUserDisplayMode;
    }

    @Override
    public void setLoggedInUserDisplayMode(LoggedInUserDisplayMode loggedInUserDisplayMode) {
        this.loggedInUserDisplayMode = loggedInUserDisplayMode;
    }

    @Override
    public boolean getShowProfileColumnsInUserList() {
        return showProfileColumnsInUserList;
    }

    @Override
    public void setShowProfileColumnsInUserList(boolean showProfileColumnsInUserList) {
        this.showProfileColumnsInUserList = showProfileColumnsInUserList;
    }

    @Override
    public EmailDisplayMode getEmailDisplayMode() {
        if (emailDisplayMode == null) {
            emailDisplayMode = EmailDisplayMode.DOMAIN_ONLY;
        }
        return emailDisplayMode;
    }

    @Override
    public void setEmailDisplayMode(EmailDisplayMode emailDisplayMode) {
        this.emailDisplayMode = emailDisplayMode;
    }

    @Override
    public boolean getRevealEmailAtClick() {
        return revealEmailAtClick;
    }

    @Override
    public void setRevealEmailAtClick(boolean revealEmailAtClick) {
        this.revealEmailAtClick = revealEmailAtClick;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((id == null) ? 0 : id.hashCode());
        result = PRIME * result + ((loggedInUserDisplayMode == null) ? 0 : loggedInUserDisplayMode.hashCode());
        result = PRIME * result + ((emailDisplayMode == null) ? 0 : emailDisplayMode.hashCode());
        result = PRIME * result + (revealEmailAtClick ? 1231 : 1237);
        result = PRIME * result + (showProfileColumnsInUserList ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof UserDetailsDisplaySettingsInfo)) return false;
        final UserDetailsDisplaySettingsInfo other = (UserDetailsDisplaySettingsInfo) obj;

        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;

        if (loggedInUserDisplayMode != other.getLoggedInUserDisplayMode()) return false;
        if (emailDisplayMode != other.getEmailDisplayMode()) return false;
        if (revealEmailAtClick != other.getRevealEmailAtClick()) return false;
        if (showProfileColumnsInUserList != other.getShowProfileColumnsInUserList()) return false;

        return true;
    }
}
