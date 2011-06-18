/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.user;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.User;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Page listing the users contained in the users.properties file 
 */
@SuppressWarnings("serial")
public class UserListProvider extends GeoServerDataProvider<User> {
    
    public static final Property<User> USERNAME = new BeanProperty<User>("username", "username");
    public static final Property<User> ROLES = new Property<User>() {

        public Comparator<User> getComparator() {
            return new PropertyComparator<User>(this);  
        }

        public IModel getModel(IModel itemModel) {
            return new Model((String) getPropertyValue((User) itemModel.getObject()));
        }

        public String getName() {
            return "roles";
        }

        public Object getPropertyValue(User item) {
            if(item.getAuthorities().length == 0)
                return "";
            
            StringBuffer sb = new StringBuffer();
            for (GrantedAuthority ga : item.getAuthorities()) {
                sb.append(ga.getAuthority());
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        public boolean isVisible() {
            return true;
        }
        
        public boolean isSearchable() {
            return true;
        };
        
    };
    public static final Property<User> ADMIN = new Property<User>() {

        public Comparator<User> getComparator() {
            return new PropertyComparator<User>(this);  
        }

        public IModel getModel(IModel itemModel) {
            return new Model((Boolean) getPropertyValue((User) itemModel.getObject()));
        }

        public String getName() {
            return "admin";
        }

        public Object getPropertyValue(User item) {
            for (GrantedAuthority ga : item.getAuthorities()) {
                if(ga.getAuthority().equals("ROLE_ADMINISTRATOR"))
                    return true;
            }
            return false;
        }

        public boolean isVisible() {
            return true;
        }
        
        public boolean isSearchable() {
            return true;
        }
        
    };
    
//    public static final Property<User> REMOVE = new PropertyPlaceholder<User>("remove");

    @Override
    protected List<User> getItems() {
        GeoserverUserDao users = (GeoserverUserDao) GeoServerApplication.get().getBean("userDetailsService");
        return users.getUsers();
    }

    @Override
    protected List<Property<User>> getProperties() {
        return Arrays.asList(USERNAME, ROLES, ADMIN);
    }

}
