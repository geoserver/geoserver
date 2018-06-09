/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Page listing for {@link GeoServerUserGroup} objects
 *
 * @author christian
 */
@SuppressWarnings("serial")
public class RoleListProvider extends GeoServerDataProvider<GeoServerRole> {

    protected String roleServiceName;

    public RoleListProvider(final String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    public static final Property<GeoServerRole> ROLENAME =
            new BeanProperty<GeoServerRole>("rolename", "authority");

    public static final String ParentPropertyName = "parentrolename";

    public class ParentProperty implements Property<GeoServerRole> {

        @Override
        public String getName() {
            return ParentPropertyName;
        }

        @Override
        public Object getPropertyValue(GeoServerRole item) {
            GeoServerRole parent = null;
            try {
                parent =
                        GeoServerApplication.get()
                                .getSecurityManager()
                                .loadRoleService(roleServiceName)
                                .getParentRole(item);
            } catch (IOException e) {
                // TODO is this correct
                throw new RuntimeException(e);
            }
            if (parent == null) return "";
            else return parent.getAuthority();
        }

        @Override
        public IModel getModel(IModel itemModel) {
            return new Model((String) getPropertyValue((GeoServerRole) itemModel.getObject()));
        }

        @Override
        public Comparator<GeoServerRole> getComparator() {
            return new PropertyComparator<GeoServerRole>(this);
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

    public static final Property<GeoServerRole> HASROLEPARAMS =
            new Property<GeoServerRole>() {

                @Override
                public String getName() {
                    return "hasroleparams";
                }

                @Override
                public Object getPropertyValue(GeoServerRole item) {
                    if (item.getProperties().size() == 0) return Boolean.FALSE;
                    else return Boolean.TRUE;
                }

                @Override
                public IModel getModel(IModel itemModel) {
                    return new Model(
                            (Boolean) getPropertyValue((GeoServerRole) itemModel.getObject()));
                }

                @Override
                public Comparator<GeoServerRole> getComparator() {
                    return new PropertyComparator<GeoServerRole>(this);
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

    @Override
    protected List<GeoServerRole> getItems() {
        SortedSet<GeoServerRole> roles = null;
        try {
            GeoServerRoleService service = null;
            if (roleServiceName != null)
                service =
                        GeoServerApplication.get()
                                .getSecurityManager()
                                .loadRoleService(roleServiceName);

            if (service == null) roles = new TreeSet<GeoServerRole>();
            else roles = service.getRoles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<GeoServerRole> roleList = new ArrayList<GeoServerRole>();
        roleList.addAll(roles);
        return roleList;
    }

    @Override
    protected List<Property<GeoServerRole>> getProperties() {
        List<Property<GeoServerRole>> result =
                new ArrayList<GeoServerDataProvider.Property<GeoServerRole>>();
        result.add(ROLENAME);
        result.add(new ParentProperty());
        result.add(HASROLEPARAMS);
        return result;
    }
}
