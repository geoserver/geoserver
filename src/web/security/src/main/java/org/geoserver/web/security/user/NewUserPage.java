/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.user;

import java.util.Collections;
import java.util.logging.Level;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


/**
 * Allows creation of a new user in users.properties
 */
@SuppressWarnings("serial")
public class NewUserPage extends AbstractUserPage {


    public NewUserPage() {
       super(new UserUIModel());
       
       username.add(new UserConflictValidator());
    }
    
    /**
     * Checks the user is not a new one
     */
    class UserConflictValidator extends AbstractValidator {

        @Override
        protected void onValidate(IValidatable validatable) {
            String newName = (String) validatable.getValue();
            try {
                GeoserverUserDao.get().loadUserByUsername(newName);
                error(validatable, "NewUserPage.userConflict", Collections.singletonMap("user", newName));
            } catch(UsernameNotFoundException e) {
                // fine, it's new, validation passed
            }
        }
        
    }
    
    @Override
    protected void onFormSubmit() {
        try {
            UserUIModel user = (UserUIModel) NewUserPage.this.getDefaultModelObject();
            GeoserverUserDao dao = GeoserverUserDao.get();
            dao.putUser(user.toSpringUser());
            dao.storeUsers();
            setResponsePage(UserPage.class);
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }
}
