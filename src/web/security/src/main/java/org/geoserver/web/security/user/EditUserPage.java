/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.user;

import java.util.logging.Level;

import org.springframework.security.userdetails.UserDetails;
import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Allows editing an existing user
 */
public class EditUserPage extends AbstractUserPage {

    public EditUserPage(UserDetails user) {
        super(new UserUIModel(user));
        username.setEnabled(false);
    }

    @Override
    protected void onFormSubmit() {
        try {
            UserUIModel model = (UserUIModel) getDefaultModelObject();
            GeoserverUserDao dao = GeoserverUserDao.get();
            dao.setUser(model.toSpringUser());
            dao.storeUsers();
            setResponsePage(UserPage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }

}
