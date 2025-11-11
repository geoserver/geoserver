/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.PaletteFormComponent;
import org.geoserver.web.GeoServerApplication;

/** A form component that can be used to edit user/rule role lists */
@SuppressWarnings("serial")
public class RolePaletteFormComponent extends PaletteFormComponent<GeoServerRole> {

    private final SubmitLink addRoleLink;

    public RolePaletteFormComponent(String id, IModel<List<GeoServerRole>> model) {
        this(id, model, new RolesModel());
    }

    public RolePaletteFormComponent(
            String id, IModel<List<GeoServerRole>> model, IModel<List<GeoServerRole>> choicesModel) {
        super(id, model, choicesModel, new ChoiceRenderer<>("authority", "authority"));
        getPalette().setLabel(new Model<>("roles"));

        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        final String roleServiceName = roleService.getName();

        if (choicesModel instanceof RuleRolesModel) add(new Label("roles", new StringResourceModel("roles", this)));
        else
            add(new Label(
                    "roles", new StringResourceModel("rolesFromActiveService", this).setParameters(roleServiceName)));

        this.addRoleLink = new SubmitLink("addRole") {
            @Override
            public void onSubmit() {
                setResponsePage(new NewRolePage(roleServiceName).setReturnPage(this.getPage()));
            }
        };
        addRoleLink.setVisible(roleService.canCreateStore());
        add(addRoleLink);
    }

    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    public void diff(Collection<GeoServerRole> orig, Collection<GeoServerRole> add, Collection<GeoServerRole> remove) {

        remove.addAll(orig);
        for (GeoServerRole role : getSelectedRoles()) {
            if (!orig.contains(role)) {
                add.add(role);
            } else {
                remove.remove(role);
            }
        }
    }

    public List<GeoServerRole> getSelectedRoles() {
        return new ArrayList<>(palette.getModelCollection());
    }

    @Override
    protected String getSelectedHeaderPropertyKey() {
        return "RolePaletteFormComponent.selectedHeader";
    }

    @Override
    protected String getAvailableHeaderPropertyKey() {
        // TODO Auto-generated method stub
        return "RolePaletteFormComponent.availableHeader";
    }

    /** Hides the add role link */
    public void hideAddRoleLink() {
        addRoleLink.setVisible(false);
    }
}
