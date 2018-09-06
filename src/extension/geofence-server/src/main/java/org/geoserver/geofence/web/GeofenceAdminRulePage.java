/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.geofence.core.model.enums.AdminGrantType;
import org.geoserver.geofence.services.dto.ShortAdminRule;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.dao.DuplicateKeyException;

public class GeofenceAdminRulePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -1652083500548496180L;

    protected DropDownChoice<String> userChoice, roleChoice, workspaceChoice;

    protected DropDownChoice<AdminGrantType> grantTypeChoice;

    public GeofenceAdminRulePage(final ShortAdminRule rule, final GeofenceAdminRulesModel rules) {

        final Form<ShortAdminRule> form =
                new Form<>("form", new CompoundPropertyModel<ShortAdminRule>(rule));
        add(form);

        form.add(new TextField<Integer>("priority").setRequired(true));

        form.add(roleChoice = new DropDownChoice<>("roleName", getRoleNames()));
        roleChoice.add(
                new OnChangeAjaxBehavior() {

                    private static final long serialVersionUID = -8846522500239968004L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        userChoice.setChoices(getUserNames(roleChoice.getConvertedInput()));
                        form.getModelObject().setUserName(null);
                        userChoice.modelChanged();
                        target.add(userChoice);
                    }
                });
        roleChoice.setNullValid(true);

        form.add(userChoice = new DropDownChoice<>("userName", getUserNames(rule.getRoleName())));
        userChoice.setOutputMarkupId(true);
        userChoice.setNullValid(true);

        form.add(workspaceChoice = new DropDownChoice<>("workspace", getWorkspaceNames()));
        workspaceChoice.setNullValid(true);

        form.add(
                grantTypeChoice =
                        new DropDownChoice<>(
                                "access",
                                Arrays.asList(AdminGrantType.values()),
                                new AdminGrantTypeRenderer()));
        grantTypeChoice.setRequired(true);

        form.add(
                new SubmitLink("save") {

                    private static final long serialVersionUID = -6524151967046867889L;

                    @Override
                    public void onSubmit() {
                        ShortAdminRule rule = (ShortAdminRule) getForm().getModelObject();
                        try {
                            rules.save(rule);
                            doReturn(GeofenceServerAdminPage.class);
                        } catch (DuplicateKeyException e) {
                            error(new ResourceModel("GeofenceRulePage.duplicate").getObject());
                        } catch (Exception exception) {
                            error(exception);
                        }
                    }
                });
        form.add(new BookmarkablePageLink<ShortAdminRule>("cancel", GeofenceServerPage.class));
    }

    protected List<String> getWorkspaceNames() {

        SortedSet<String> resultSet = new TreeSet<String>();
        for (WorkspaceInfo ws : getCatalog().getFacade().getWorkspaces()) {
            resultSet.add(ws.getName());
        }
        return new ArrayList<>(resultSet);
    }

    protected List<String> getRoleNames() {
        SortedSet<String> resultSet = new TreeSet<>();
        try {
            for (GeoServerRole role : securityManager().getRolesForAccessControl()) {
                resultSet.add(role.getAuthority());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        return new ArrayList<>(resultSet);
    }

    protected List<String> getUserNames(String roleName) {
        SortedSet<String> resultSet = new TreeSet<>();
        GeoServerSecurityManager securityManager = securityManager();
        try {
            if (roleName == null) {
                for (String serviceName : securityManager.listUserGroupServices()) {
                    for (GeoServerUser user :
                            securityManager.loadUserGroupService(serviceName).getUsers()) {
                        resultSet.add(user.getUsername());
                    }
                }
            } else {
                for (String serviceName : securityManager.listRoleServices()) {
                    GeoServerRoleService roleService = securityManager.loadRoleService(serviceName);
                    GeoServerRole role = roleService.getRoleByName(roleName);
                    if (role != null) {
                        resultSet.addAll(roleService.getUserNamesForRole(role));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        return new ArrayList<>(resultSet);
    }

    protected class AdminGrantTypeRenderer extends ChoiceRenderer<AdminGrantType> {

        private static final long serialVersionUID = -7146780173551842734L;

        public Object getDisplayValue(AdminGrantType object) {
            return new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(AdminGrantType object, int index) {
            return object.name();
        }
    }

    protected GeoServerSecurityManager securityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }
}
