/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.DEFAULT_ADMIN_PASSWD;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.web.passwd.MasterPasswordChangePage;
import org.geoserver.security.web.user.EditUserPage;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.geotools.util.logging.Logging;

public class SecurityHomePageContentProvider implements GeoServerHomePageContentProvider {

    static Logger LOGGER = Logging.getLogger(SecurityHomePageContentProvider.class);

    @Override
    public Component getPageBodyComponent(String id) {
        // do a check that the root password is not set
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        if (secMgr.checkAuthenticationForAdminRole()) {
            return new SecurityWarningsPanel(id);
        }
        return null;
    }

    // PasswordChangeWarningPanel
    static class SecurityWarningsPanel extends Panel {

        public SecurityWarningsPanel(String id) {
            super(id);

            GeoServerSecurityManager manager = GeoServerApplication.get().getSecurityManager();

            // warn in case of an existing masterpw.info
            Resource mpInfo = null;
            Label mpInfoLabel = null;
            try {
                mpInfo =
                        manager.get("security")
                                .get(GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME);
                mpInfoLabel =
                        new Label(
                                "mpfile",
                                new StringResourceModel("masterPasswordFile", (Component) this)
                                        .setParameters(mpInfo.path()));
                mpInfoLabel.setEscapeModelStrings(false);
                add(mpInfoLabel);
                mpInfoLabel.setVisible(Resources.exists(mpInfo));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // warn in case of an existing user.properties.old
            Resource userprops = null;
            Label userpropsLabel = null;
            try {
                userprops = manager.get("security").get("users.properties.old");
                userpropsLabel =
                        new Label(
                                "userpropsold",
                                new StringResourceModel("userPropertiesOldFile", (Component) this)
                                        .setParameters(userprops.path()));
                userpropsLabel.setEscapeModelStrings(false);
                add(userpropsLabel);
                userpropsLabel.setVisible(Resources.exists(userprops));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // check for default master password
            boolean visibility = manager.checkMasterPassword(DEFAULT_ADMIN_PASSWD, false);

            Label label =
                    new Label(
                            "mpmessage",
                            new StringResourceModel(
                                    "changeMasterPassword", (Component) this, null));
            label.setEscapeModelStrings(false);
            add(label);
            Link link = null;
            add(
                    link =
                            new Link("mplink") {
                                @Override
                                public void onClick() {
                                    setResponsePage(new MasterPasswordChangePage());
                                }
                            });
            label.setVisible(visibility);
            link.setVisible(visibility);

            // check for default admin password
            visibility = manager.checkForDefaultAdminPassword();
            Page changeItPage = null;
            String passwordEncoderName = null;
            try {
                GeoServerUserGroupService ugService =
                        manager.loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);
                if (ugService != null) {
                    passwordEncoderName = ugService.getPasswordEncoderName();
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

            final Page linkPage = changeItPage;
            label =
                    new Label(
                            "adminmessage",
                            new StringResourceModel("changeAdminPassword", (Component) this, null));
            label.setEscapeModelStrings(false);
            add(label);
            add(
                    link =
                            new Link("adminlink") {
                                @Override
                                public void onClick() {
                                    setResponsePage(linkPage);
                                }
                            });
            label.setVisible(visibility);
            link.setVisible(visibility);

            // inform about strong encryption
            if (manager.isStrongEncryptionAvailable()) {
                add(
                        new Label(
                                        "strongEncryptionMsg",
                                        new StringResourceModel(
                                                "strongEncryption",
                                                new SecuritySettingsPage(),
                                                null))
                                .add(new AttributeAppender("class", new Model("info-link"), " ")));
            } else {
                add(
                        new Label(
                                        "strongEncryptionMsg",
                                        new StringResourceModel(
                                                "noStrongEncryption",
                                                new SecuritySettingsPage(),
                                                null))
                                .add(
                                        new AttributeAppender(
                                                "class", new Model("warning-link"), " ")));
            }

            // check for password encoding in the default user group service
            visibility = false;
            if (passwordEncoderName != null) {
                GeoServerPasswordEncoder encoder = manager.loadPasswordEncoder(passwordEncoderName);
                if (encoder != null) {
                    visibility = encoder.isReversible();
                }
            }

            label =
                    new Label(
                            "digestEncoding",
                            new StringResourceModel("digestEncoding", (Component) this, null));
            add(label);
            label.setVisible(visibility);
        }
    }
}
