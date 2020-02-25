/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleHierarchyHelper;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.property.PropertyEditorFormComponent;
import org.springframework.util.StringUtils;

/** Allows creation of a new user in users.properties */
@SuppressWarnings("serial")
public abstract class AbstractRolePage extends AbstractSecurityPage {

    String roleServiceName;

    protected AbstractRolePage(String roleService, GeoServerRole role) {
        this.roleServiceName = roleService;
        boolean hasRoleStore = hasRoleStore(roleServiceName);

        if (role == null) role = new GeoServerRole("");

        Form form = new Form("form", new CompoundPropertyModel(role));
        add(form);

        StringResourceModel descriptionModel;
        if (role.getUserName() != null) {
            descriptionModel =
                    new StringResourceModel("personalizedRole", getPage())
                            .setParameters(role.getUserName());
        } else {
            descriptionModel = new StringResourceModel("anonymousRole", getPage());
        }
        form.add(new Label("description", descriptionModel));

        form.add(
                new TextField("name", new Model(role.getAuthority()))
                        .setRequired(true)
                        .setEnabled(hasRoleStore));
        form.add(
                new DropDownChoice("parent", new ParentRoleModel(role), new ParentRolesModel(role))
                        .setNullValid(true)
                        .setEnabled(hasRoleStore));
        form.add(new PropertyEditorFormComponent("properties").setEnabled(hasRoleStore));

        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            onFormSubmit((GeoServerRole) getForm().getModelObject());
                            setReturnPageDirtyAndReturn(true);
                        } catch (IOException e) {
                            if (e.getCause() instanceof AbstractSecurityException) {
                                error(e.getCause());
                            } else {
                                error(
                                        new ParamResourceModel(
                                                        "saveError", getPage(), e.getMessage())
                                                .getObject());
                            }
                            LOGGER.log(Level.SEVERE, "Error occurred while saving role", e);
                        }
                    }
                }.setVisible(hasRoleStore));

        form.add(getCancelLink());
    }

    class ParentRoleModel extends LoadableDetachableModel<String> {
        GeoServerRole role;

        ParentRoleModel(GeoServerRole role) {
            this.role = role;
        }

        @Override
        protected String load() {
            try {
                GeoServerRole parentRole = getRoleService(roleServiceName).getParentRole(role);
                return parentRole != null ? parentRole.getAuthority() : null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ParentRolesModel implements IModel<List<String>> {

        List<String> parentRoles;

        ParentRolesModel(GeoServerRole role) {
            try {
                parentRoles = computeAllowableParentRoles(role);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> computeAllowableParentRoles(GeoServerRole role) throws IOException {
            Map<String, String> parentMappings =
                    getRoleService(roleServiceName).getParentMappings();
            // if no parent mappings, return empty list
            if (parentMappings == null || parentMappings.isEmpty()) return Collections.emptyList();

            if (role != null && StringUtils.hasLength(role.getAuthority())) {
                // filter out roles already used as parents
                RoleHierarchyHelper helper = new RoleHierarchyHelper(parentMappings);

                Set<String> parents = new HashSet<String>(parentMappings.keySet());
                parents.removeAll(helper.getDescendants(role.getAuthority()));
                parents.remove(role.getAuthority());
                return new ArrayList(parents);

            } else {
                // no rolename given, we are creating a new one
                return new ArrayList(parentMappings.keySet());
            }
        }

        @Override
        public List<String> getObject() {
            return parentRoles;
        }

        @Override
        public void setObject(List<String> object) {}

        @Override
        public void detach() {}
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(GeoServerRole role) throws IOException;
}
