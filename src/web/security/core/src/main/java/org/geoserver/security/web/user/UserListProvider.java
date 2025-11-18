/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.config.impl.UserDetailsDisplaySettingsInfoImpl;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserProfilePropertyNames;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/** Page listing the users contained in the users.properties file */
@SuppressWarnings("serial")
public class UserListProvider extends GeoServerDataProvider<GeoServerUser> {

    public static final Property<GeoServerUser> USERNAME = new BeanProperty<>("username", "username");
    public static final Property<GeoServerUser> ENABLED = new BeanProperty<>("enabled", "enabled");
    public static final Property<GeoServerUser> FIRST_NAME =
            new GeoServerUserPropProperty(UserProfilePropertyNames.FIRST_NAME);
    public static final Property<GeoServerUser> LAST_NAME =
            new GeoServerUserPropProperty(UserProfilePropertyNames.LAST_NAME);
    public static final Property<GeoServerUser> PREFERRED_USERNAME =
            new GeoServerUserPropProperty(UserProfilePropertyNames.PREFERRED_USERNAME);
    public static final Property<GeoServerUser> EMAIL = new GeoServerUserPropProperty(UserProfilePropertyNames.EMAIL);

    private static final List<Property<GeoServerUser>> PROFILE_COLUMNS =
            List.of(FIRST_NAME, LAST_NAME, PREFERRED_USERNAME, EMAIL);

    protected String userGroupServiceName;

    public UserListProvider(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }
    /*
        public static final Property<GeoserverUser> ROLES = new Property<GeoserverUser>() {

            public Comparator<GeoserverUser> getComparator() {
                return new PropertyComparator<GeoserverUser>(this);
            }

            public IModel getModel(IModel itemModel) {
                return new Model((String) getPropertyValue((GeoserverUser) itemModel.getObject()));
            }

            public String getName() {
                return "roles";
            }

            public Object getPropertyValue(GeoserverUser item) {
                if(item.getAuthorities().size() == 0)
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

    */
    public static final Property<GeoServerUser> HASATTRIBUTES = new Property<>() {

        @Override
        public String getName() {
            return "hasattributes";
        }

        @Override
        public Object getPropertyValue(GeoServerUser item) {
            if (item.getProperties().isEmpty()) return Boolean.FALSE;
            else return Boolean.TRUE;
        }

        @Override
        public IModel getModel(IModel itemModel) {
            return new Model<>((Boolean) getPropertyValue((GeoServerUser) itemModel.getObject()));
        }

        @Override
        public Comparator<GeoServerUser> getComparator() {
            return new PropertyComparator<>(this);
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public boolean isSearchable() {
            return true;
        }
    };

    /*
        public static final Property<GeoserverUser> ADMIN = new Property<GeoserverUser>() {

            public Comparator<GeoserverUser> getComparator() {
                return new PropertyComparator<GeoserverUser>(this);
            }

            public IModel getModel(IModel itemModel) {
                return new Model((Boolean) getPropertyValue((GeoserverUser) itemModel.getObject()));
            }

            public String getName() {
                return "admin";
            }

            public Object getPropertyValue(GeoserverUser item) {
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
    */

    //    public static final Property<User> REMOVE = new PropertyPlaceholder<User>("remove");

    @Override
    protected List<GeoServerUser> getItems() {
        SortedSet<GeoServerUser> users = null;
        try {
            GeoServerUserGroupService service = null;
            if (userGroupServiceName != null)
                service = getApplication().getSecurityManager().loadUserGroupService(userGroupServiceName);

            if (service == null) users = new TreeSet<>();
            else users = service.getUsers();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<GeoServerUser> userList = new ArrayList<>();
        userList.addAll(users);
        return userList;
    }

    @Override
    protected List<Property<GeoServerUser>> getProperties() {
        List<Property<GeoServerUser>> result = new ArrayList<>();
        result.add(USERNAME);
        result.add(ENABLED);
        result.add(HASATTRIBUTES);
        //        result.add(ROLES);
        //        result.add(ADMIN);
        if (shouldShowProfileColumns()) {
            result.addAll(PROFILE_COLUMNS);
        }
        return result;
    }

    private boolean shouldShowProfileColumns() {
        UserDetailsDisplaySettingsInfo userDetailsDisplaySettingsInfo = Optional.ofNullable(
                        GeoServerApplication.get().getGeoServer().getGlobal().getUserDetailsDisplaySettings())
                .orElse(new UserDetailsDisplaySettingsInfoImpl());
        return userDetailsDisplaySettingsInfo.getShowProfileColumnsInUserList();
    }

    private static class GeoServerUserPropProperty implements Property<GeoServerUser> {

        private final String propertyName;

        public GeoServerUserPropProperty(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public String getName() {
            return propertyName;
        }

        @Override
        public Object getPropertyValue(GeoServerUser item) {
            return item.getProperties().getProperty(propertyName, "");
        }

        @Override
        public IModel<String> getModel(IModel itemModel) {
            return new Model<>((String) getPropertyValue((GeoServerUser) itemModel.getObject()));
        }

        @Override
        public Comparator<GeoServerUser> getComparator() {
            return new PropertyComparator<>(this);
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public boolean isSearchable() {
            return true;
        }
    }
}
