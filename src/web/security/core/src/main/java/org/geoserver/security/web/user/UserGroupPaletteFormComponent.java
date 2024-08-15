/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.PaletteFormComponent;
import org.geoserver.security.web.group.NewGroupPage;
import org.geoserver.web.GeoServerApplication;

/** A form component that can be used to edit user to group assignments */
public class UserGroupPaletteFormComponent extends PaletteFormComponent<GeoServerUserGroup> {

    private static final long serialVersionUID = 1L;

    GeoServerUser user;

    public UserGroupPaletteFormComponent(String id, String ugServiceName, GeoServerUser user) {
        this(id, new SelectedGroupsModel(ugServiceName, user), ugServiceName, user);
    }

    public UserGroupPaletteFormComponent(
            String id,
            IModel<List<GeoServerUserGroup>> model,
            final String ugServiceName,
            GeoServerUser user) {
        super(
                id,
                model,
                new GroupsModel(ugServiceName),
                new ChoiceRenderer<>("groupname", "groupname"));

        add(
                new SubmitLink("addGroup") {
                    @Override
                    public void onSubmit() {
                        setResponsePage(
                                new NewGroupPage(ugServiceName).setReturnPage(this.getPage()));
                    }
                });
    }

    public List<GeoServerUserGroup> getSelectedGroups() {
        return new ArrayList<>(palette.getModelCollection());
    }

    public void diff(
            Collection<GeoServerUserGroup> orig,
            Collection<GeoServerUserGroup> add,
            Collection<GeoServerUserGroup> remove) {

        remove.addAll(orig);
        for (GeoServerUserGroup group : getSelectedGroups()) {
            if (!orig.contains(group)) {
                add.add(group);
            } else {
                remove.remove(group);
            }
        }
    }

    static class SelectedGroupsModel implements IModel<List<GeoServerUserGroup>> {
        List<GeoServerUserGroup> groups;

        public SelectedGroupsModel(String ugServiceName, GeoServerUser user) {
            try {
                GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
                setObject(
                        new ArrayList<>(
                                secMgr.loadUserGroupService(ugServiceName).getGroupsForUser(user)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<GeoServerUserGroup> getObject() {
            return groups;
        }

        @Override
        public void setObject(List<GeoServerUserGroup> object) {
            this.groups = object;
        }

        @Override
        public void detach() {}
    }

    @Override
    protected String getSelectedHeaderPropertyKey() {
        return "UserGroupPaletteFormComponent.selectedHeader";
    }

    @Override
    protected String getAvailableHeaderPropertyKey() {
        return "UserGroupPaletteFormComponent.availableHeader";
    }
}
