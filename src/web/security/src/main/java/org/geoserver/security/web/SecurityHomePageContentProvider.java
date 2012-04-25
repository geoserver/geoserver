/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.DEFAULT_ADMIN_PASSWD;
import static org.geoserver.security.impl.GeoServerUser.ROOT_USERNAME;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.web.passwd.MasterPasswordChangePage;
import org.geoserver.security.web.user.EditUserPage;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class SecurityHomePageContentProvider implements
        GeoServerHomePageContentProvider {

    static Logger LOGGER = Logging.getLogger(SecurityHomePageContentProvider.class);

    @Override
    public Component getPageBodyComponent(String id) {
        //do a check that the root password is not set
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        if (secMgr.checkAuthenticationForAdminRole()) {
            if (testAuthentication(ROOT_USERNAME, DEFAULT_ADMIN_PASSWD, secMgr)) {
                return new PasswordChangeWarningPanel(id, "changeMasterPassword",
                    new MasterPasswordChangePage());
            }
            else if (testAuthentication(ADMIN_USERNAME, "geoserver", secMgr)) {
                Page changeItPage = null;
                try {
                    GeoServerUserGroupService ugService = secMgr.loadUserGroupService("default");
                    if (ugService != null) {
                        GeoServerUser user = ugService.getUserByUsername(ADMIN_USERNAME);
                        if (user != null) {
                            changeItPage = new EditUserPage(ugService.getName(), user);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error looking up admin user", e);
                }
                if (changeItPage == null) {
                    changeItPage = new UserGroupRoleServicesPage();
                }
                return new PasswordChangeWarningPanel(id, "changeAdminPassword", changeItPage);
            }
        }

        return null;
    }

    boolean testAuthentication(String user, String passwd, GeoServerSecurityManager secMgr) {
        Authentication token = new UsernamePasswordAuthenticationToken(user, passwd);
        try {
            token = secMgr.authenticate(token);
        }
        catch(Exception e) {
            //ok
        }
        return token.isAuthenticated();
    }

    static class PasswordChangeWarningPanel extends Panel {

        public PasswordChangeWarningPanel(String id, String messageKey, final Page changeItPage) {
            super(id);

            add(new Label("message", new StringResourceModel(messageKey, (Component)this, null))
                .setEscapeModelStrings(false));
            add(new Link("link") {
                @Override
                public void onClick() {
                    setResponsePage(changeItPage);
                }
            });
        }
    }
}
