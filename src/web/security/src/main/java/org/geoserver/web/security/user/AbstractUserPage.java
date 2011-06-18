/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualInputValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.RolesFormComponent;

/**
 * Allows creation of a new user in users.properties
 */
@SuppressWarnings("serial")
public abstract class AbstractUserPage extends GeoServerSecuredPage {
    TextField username;

    protected AbstractUserPage(UserUIModel user) {
        final Model userModel = new Model(user);

        // build the form
        Form form = new Form("userForm");
        form.setModel(new CompoundPropertyModel(userModel));
        setDefaultModel(userModel);
        add(form);
        
        // populate the form editing components
        username = new TextField("username");
        form.add(username);
        PasswordTextField pw1 = new PasswordTextField("password").setResetPassword(false);
        PasswordTextField pw2 = new PasswordTextField("confirmPassword").setResetPassword(false);
        form.add(pw1);
        form.add(pw2);
        form.add(new RolesFormComponent("roles", new PropertyModel(userModel, "authorities"), form, false));
        
        // build the submit/cancel
        form.add(new BookmarkablePageLink("cancel", UserPage.class));
        form.add(saveLink());
        
        // add the validators
        form.add(new EqualInputValidator(pw1, pw2));
        username.setRequired(true);
    }

    SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                onFormSubmit();
            }
        };
    }
    
    /**
     * Implements the actual save action
     */
    protected abstract void onFormSubmit();
    
    /**
     * Mediates between the UI and the Spring User class
     */
    static class UserUIModel implements Serializable {
        String username;

        /**
         * Will be used to check if the user edited the pw in case the pw is encrypted with a one
         * way only (digest) encryption algorithm
         */
        String originalPassword;

        String password;

        String confirmPassword;

        List<String> authorities;

        boolean enabled;

        /**
         * Maps a {@link UserDetails} into something that maps 1-1 with the UI
         * 
         * @param springUser
         */
        public UserUIModel(UserDetails springUser) {
            this.username = springUser.getUsername();
            this.originalPassword = springUser.getPassword();
            this.password = this.originalPassword;
            this.confirmPassword = this.originalPassword;
            this.authorities = toRoleList(springUser);
            this.enabled = springUser.isEnabled();
        }

        /**
         * Prepares for an emtpy new user
         */
        public UserUIModel() {
            this.authorities = new ArrayList<String>();
            this.enabled = true;
        }

        /**
         * Converts this UI view back into an Spring {@link User} object
         * 
         * @return
         */
        public User toSpringUser() {
            return new User(username, password, enabled, true, true, true,
                    toGrantedAuthorities(authorities));
        }

        /**
         * From {@link GrantedAuthority}[] to {@link List}<String>
         */
        List<String> toRoleList(UserDetails springUser) {
            List<String> result = new ArrayList<String>(springUser.getAuthorities().length);
            for (GrantedAuthority ga : springUser.getAuthorities()) {
                result.add(ga.getAuthority());
            }
            return result;
        }

        /**
         * From to {@link List}<String> {@link GrantedAuthority}[]
         */

        GrantedAuthority[] toGrantedAuthorities(List<String> roles) {
            GrantedAuthority[] authorities = new GrantedAuthority[roles.size()];
            for (int i = 0; i < authorities.length; i++) {
                authorities[i] = new GrantedAuthorityImpl(roles.get(i));
            }
            return authorities;
        }
    }
}
