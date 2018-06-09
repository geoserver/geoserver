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
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.PaletteFormComponent;
import org.geoserver.web.GeoServerApplication;

/** A form component that can be used to edit user/rule role lists */
@SuppressWarnings("serial")
public class RolePaletteFormComponent extends PaletteFormComponent<GeoServerRole> {

    public RolePaletteFormComponent(String id, IModel<List<GeoServerRole>> model) {
        this(id, model, new RolesModel());
    }

    public RolePaletteFormComponent(
            String id,
            IModel<List<GeoServerRole>> model,
            IModel<Collection<GeoServerRole>> choicesModel) {
        super(id, model, choicesModel, new ChoiceRenderer<GeoServerRole>("authority", "authority"));

        //        rolePalette = new Palette<GeoServerRole>(
        //                "roles", , choicesModel,
        //                , 10, false) {
        //            // trick to force the palette to have at least one selected elements
        //            // tried with a nicer validator but it's not used at all, the required thing
        //            // instead is working (don't know why...)
        //            protected Recorder<GeoServerRole> newRecorderComponent() {
        //                Recorder<GeoServerRole> rec = super.newRecorderComponent();
        //                //add any behaviours that need to be added
        //                rec.add(toAdd.toArray(new Behavior[toAdd.size()]));
        //                toAdd.clear();
        //                /*if (isRequired)
        //                    rec.setRequired(true);
        //                if (behavior!=null)
        //                    rec.add(behavior);*/
        //                return rec;
        //            }
        //        };

        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        final String roleServiceName = roleService.getName();

        if (choicesModel instanceof RuleRolesModel)
            add(new Label("roles", new StringResourceModel("roles", this)));
        else
            add(
                    new Label(
                            "roles",
                            new StringResourceModel("rolesFromActiveService", this)
                                    .setParameters(roleServiceName)));

        add(
                new SubmitLink("addRole") {
                    @Override
                    public void onSubmit() {
                        setResponsePage(
                                new NewRolePage(roleServiceName).setReturnPage(this.getPage()));
                    }
                }.setVisible(roleService.canCreateStore()));
    }

    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    public void diff(
            Collection<GeoServerRole> orig,
            Collection<GeoServerRole> add,
            Collection<GeoServerRole> remove) {

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
        return new ArrayList(palette.getModelCollection());
    }

    @Override
    protected String getSelectedHeaderPropertyKey() {
        return "RolePaletteFormComponent.selectedHeader";
    }

    @Override
    protected String getAvaliableHeaderPropertyKey() {
        // TODO Auto-generated method stub
        return "RolePaletteFormComponent.availableHeader";
    }
}
