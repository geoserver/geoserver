/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.GeoServerRole;

/** A form component that can be used to edit user/rule role lists */
@SuppressWarnings("serial")
public class RuleRolesFormComponent extends RolePaletteFormComponent {

    static final Set<String> ANY_ROLE = Collections.singleton("*");

    public RuleRolesFormComponent(String id, IModel<Collection<String>> roleNamesModel) {
        super(id, new RoleNamesModel(roleNamesModel), new RuleRolesModel());

        boolean anyRolesEnabled = ANY_ROLE.equals(roleNamesModel.getObject());
        add(new AjaxCheckBox("anyRole", new Model<>(anyRolesEnabled)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                palette.setEnabled(!getModelObject());
                target.add(palette);
            }
        });
        palette.setEnabled(!anyRolesEnabled);
    }

    public RuleRolesFormComponent setHasAnyRole(boolean hasAny) {
        get("anyRole").setDefaultModelObject(hasAny);
        palette.setEnabled(!hasAny);
        return this;
    }

    public boolean isHasAnyRole() {
        return (Boolean) get("anyRole").getDefaultModelObject();
    }

    @Override
    protected String getSelectedHeaderPropertyKey() {
        return "RuleRolesFormComponent.selectedHeader";
    }

    @Override
    protected String getAvailableHeaderPropertyKey() {
        return "RuleRolesFormComponent.availableHeader";
    }

    public Set<GeoServerRole> getRolesForStoring() {
        Set<GeoServerRole> result = new HashSet<>();
        if (isHasAnyRole()) {
            result.add(GeoServerRole.ANY_ROLE);
        } else {
            result.addAll(getSelectedRoles());
        }
        return result;
    }

    public Set<String> getRolesNamesForStoring() {
        Set<String> result = new HashSet<>();
        for (GeoServerRole role : getRolesForStoring()) {
            result.add(role.getAuthority());
        }
        return result;
    }
}
