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
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.wicket.GeoServerDataProvider;

/** Page listing the users contained in the users.properties file */
@SuppressWarnings("serial")
public class UserListProvider extends GeoServerDataProvider<GeoServerUser> {

    public static final Property<GeoServerUser> USERNAME =
            new BeanProperty<GeoServerUser>("username", "username");
    public static final Property<GeoServerUser> ENABLED =
            new BeanProperty<GeoServerUser>("enabled", "enabled");
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
    public static final Property<GeoServerUser> HASATTRIBUTES =
            new Property<GeoServerUser>() {

                @Override
                public String getName() {
                    return "hasattributes";
                }

                @Override
                public Object getPropertyValue(GeoServerUser item) {
                    if (item.getProperties().size() == 0) return Boolean.FALSE;
                    else return Boolean.TRUE;
                }

                @Override
                public IModel getModel(IModel itemModel) {
                    return new Model(
                            (Boolean) getPropertyValue((GeoServerUser) itemModel.getObject()));
                }

                @Override
                public Comparator<GeoServerUser> getComparator() {
                    return new PropertyComparator<GeoServerUser>(this);
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
                service =
                        getApplication()
                                .getSecurityManager()
                                .loadUserGroupService(userGroupServiceName);

            if (service == null) users = new TreeSet<GeoServerUser>();
            else users = service.getUsers();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<GeoServerUser> userList = new ArrayList<GeoServerUser>();
        userList.addAll(users);
        return userList;
    }

    @Override
    protected List<Property<GeoServerUser>> getProperties() {
        List<Property<GeoServerUser>> result =
                new ArrayList<GeoServerDataProvider.Property<GeoServerUser>>();
        result.add(USERNAME);
        result.add(ENABLED);
        result.add(HASATTRIBUTES);
        //        result.add(ROLES);
        //        result.add(ADMIN);
        return result;
    }
}
