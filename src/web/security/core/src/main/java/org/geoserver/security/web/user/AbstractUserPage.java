/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.form.validation.EqualInputValidator;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.GroupAdminProperty;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.password.GeoServerEmptyPasswordEncoder;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.EditRolePage;
import org.geoserver.security.web.role.RoleListProvider;
import org.geoserver.security.web.role.RolePaletteFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.property.PropertyEditorFormComponent;

/** Allows creation of a new user in users.properties */
public abstract class AbstractUserPage extends AbstractSecurityPage {

    protected RolePaletteFormComponent rolePalette;
    protected UserGroupPaletteFormComponent userGroupPalette;
    protected UserGroupListMultipleChoice adminGroupChoice;
    protected ListView<GeoServerRole> calculatedRoles;

    protected String ugServiceName;

    protected AbstractUserPage(String ugServiceName, final GeoServerUser user) {
        this.ugServiceName = ugServiceName;

        GeoServerUserGroupService ugService = getUserGroupService(ugServiceName);
        boolean emptyPasswd =
                getSecurityManager().loadPasswordEncoder(ugService.getPasswordEncoderName())
                        instanceof GeoServerEmptyPasswordEncoder;
        boolean hasUserGroupStore = ugService.canCreateStore();
        boolean hasRoleStore = hasRoleStore(getSecurityManager().getActiveRoleService().getName());

        // build the form
        Form form = new Form<Serializable>("form", new CompoundPropertyModel(user));
        add(form);

        form.add(new TextField("username").setEnabled(hasUserGroupStore));
        form.add(new CheckBox("enabled").setEnabled(hasUserGroupStore));

        PasswordTextField pw1 =
                new PasswordTextField("password") {
                    @Override
                    public boolean isRequired() {
                        return isFinalSubmit(this);
                    }
                };
        form.add(pw1);
        pw1.setResetPassword(false);
        pw1.setEnabled(hasUserGroupStore && !emptyPasswd);

        PasswordTextField pw2 =
                new PasswordTextField("confirmPassword", new Model(user.getPassword())) {
                    @Override
                    public boolean isRequired() {
                        return isFinalSubmit(this);
                    }
                };
        form.add(pw2);
        pw2.setResetPassword(false);
        pw2.setEnabled(hasUserGroupStore && !emptyPasswd);

        form.add(new PropertyEditorFormComponent("properties").setEnabled(hasUserGroupStore));

        form.add(
                userGroupPalette =
                        new UserGroupPaletteFormComponent("groups", ugServiceName, user));
        userGroupPalette.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateCalculatedRoles(target);
                    }
                });
        userGroupPalette.setEnabled(hasUserGroupStore);

        List<GeoServerRole> roles;
        try {
            roles =
                    new ArrayList(
                            getSecurityManager()
                                    .getActiveRoleService()
                                    .getRolesForUser(user.getUsername()));
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

        form.add(rolePalette = new RolePaletteFormComponent("roles", new ListModel(roles)));
        rolePalette.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateCalculatedRoles(target);
                        updateGroupAdminList(target);
                    }
                });
        rolePalette.setOutputMarkupId(true);
        rolePalette.setEnabled(hasRoleStore);

        boolean isGroupAdmin = roles.contains(GeoServerRole.GROUP_ADMIN_ROLE);
        List<GeoServerUserGroup> adminGroups = new ArrayList();
        if (isGroupAdmin) {
            for (String groupName : GroupAdminProperty.get(user.getProperties())) {
                try {
                    adminGroups.add(ugService.getGroupByGroupname(groupName));
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }
            }
        }

        form.add(
                adminGroupChoice =
                        new UserGroupListMultipleChoice(
                                "adminGroups",
                                new ListModel(adminGroups),
                                new GroupsModel(ugServiceName)));
        adminGroupChoice.setOutputMarkupId(true);
        adminGroupChoice.setEnabled(hasUserGroupStore && isGroupAdmin);

        WebMarkupContainer container = new WebMarkupContainer("calculatedRolesContainer");
        form.add(container);
        container.setOutputMarkupId(true);

        container.add(
                calculatedRoles =
                        new ListView<GeoServerRole>(
                                "calculatedRoles", new CalculatedRoleModel(user)) {
                            @Override
                            @SuppressWarnings("unchecked")
                            protected void populateItem(ListItem<GeoServerRole> item) {
                                IModel<GeoServerRole> model = item.getModel();
                                item.add(
                                        new SimpleAjaxLink(
                                                "role",
                                                model,
                                                RoleListProvider.ROLENAME.getModel(model)) {
                                            @Override
                                            protected void onClick(AjaxRequestTarget target) {
                                                setResponsePage(
                                                        new EditRolePage(
                                                                        getSecurityManager()
                                                                                .getActiveRoleService()
                                                                                .getName(),
                                                                        (GeoServerRole)
                                                                                getDefaultModelObject())
                                                                .setReturnPage(this.getPage()));
                                            }
                                        });
                            }
                        });
        calculatedRoles.setOutputMarkupId(true);

        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            // update the user property listing the group names the user is admin
                            // for
                            if (adminGroupChoice.isEnabled()) {
                                Collection<GeoServerUserGroup> groups =
                                        adminGroupChoice.getModelObject();
                                String[] groupNames = new String[groups.size()];
                                int i = 0;
                                for (GeoServerUserGroup group : groups) {
                                    groupNames[i++] = group.getGroupname();
                                }

                                GroupAdminProperty.set(user.getProperties(), groupNames);
                            } else {
                                GroupAdminProperty.del(user.getProperties());
                            }

                            onFormSubmit(user);
                            setReturnPageDirtyAndReturn(true);
                        } catch (Exception e) {
                            handleSubmitError(e);
                        }
                    }
                }.setEnabled(
                        hasUserGroupStore
                                || hasRoleStore(
                                        getSecurityManager().getActiveRoleService().getName())));
        form.add(getCancelLink());

        // add the validators
        form.add(
                new EqualInputValidator(pw1, pw2) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void validate(Form<?> form) {
                        if (isFinalSubmit(form)) {
                            super.validate(form);
                        }
                    }

                    @Override
                    protected String resourceKey() {
                        return "AbstractUserPage.passwordMismatch";
                    }
                });
        form.add(new GroupAdminValidator());
    }

    boolean isFinalSubmit(FormComponent component) {
        return isFinalSubmit(Form.findForm(component));
    }

    boolean isFinalSubmit(Form form) {
        if (form == null) {
            return false;
        }
        return form.findSubmittingButton() == form.get("save");
    }

    void updateCalculatedRoles(AjaxRequestTarget target) {
        calculatedRoles.modelChanged();
        target.add(calculatedRoles.getParent());
    }

    void updateGroupAdminList(AjaxRequestTarget target) {
        adminGroupChoice.setEnabled(
                rolePalette.getSelectedRoles().contains(GeoServerRole.GROUP_ADMIN_ROLE));
        target.add(adminGroupChoice);
    }

    void handleSubmitError(Exception e) {
        LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);

        if (e instanceof RuntimeException && e.getCause() instanceof Exception) {
            e = (Exception) e.getCause();
        }

        if (e instanceof IOException && e.getCause() instanceof AbstractSecurityException) {
            e = (Exception) e.getCause();
        }

        if (e instanceof AbstractSecurityException) {
            error(e);
        } else {
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()).getObject());
        }
    }

    /**
     * List model that calculates derived roles for the user, those assigned directly and through
     * group membership.
     */
    class CalculatedRoleModel extends LoadableDetachableModel<List<GeoServerRole>> {

        GeoServerUser user;

        CalculatedRoleModel(GeoServerUser user) {
            this.user = user;
        }

        @Override
        protected List<GeoServerRole> load() {
            List<GeoServerRole> tmp = new ArrayList<GeoServerRole>();
            List<GeoServerRole> result = new ArrayList<GeoServerRole>();
            try {
                GeoServerUserGroupService ugService =
                        getSecurityManager().loadUserGroupService(ugServiceName);
                GeoServerRoleService gaService = getSecurityManager().getActiveRoleService();

                RoleCalculator calc = new RoleCalculator(ugService, gaService);
                tmp.addAll(rolePalette.getSelectedRoles());
                calc.addInheritedRoles(tmp);

                for (GeoServerUserGroup group : userGroupPalette.getSelectedGroups()) {
                    if (group.isEnabled()) {
                        tmp.addAll(calc.calculateRoles(group));
                    }
                }
                result.addAll(calc.personalizeRoles(user, tmp));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Collections.sort(result);
            return result;
        }
    }

    /**
     * Validator that ensures when a user is assigned to be a group admin that at least one group is
     * selected.
     */
    class GroupAdminValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {adminGroupChoice};
        }

        @Override
        public void validate(Form<?> form) {
            if (adminGroupChoice.isEnabled()) {
                adminGroupChoice.updateModel();
                if (adminGroupChoice.getModelObject().isEmpty()) {
                    form.error(
                            new StringResourceModel("noAdminGroups", getPage(), null).getString());
                }
            }
        }
    }

    /** Implements the actual save action. */
    protected abstract void onFormSubmit(GeoServerUser user)
            throws IOException, PasswordPolicyException;
}
