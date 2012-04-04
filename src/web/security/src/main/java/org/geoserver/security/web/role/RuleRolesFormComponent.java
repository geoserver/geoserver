/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerApplication;

/**
 * A form component that can be used to edit user/rule role lists
 */
@SuppressWarnings("serial")
public class RuleRolesFormComponent extends RolePaletteFormComponent {

    public RuleRolesFormComponent(String id, IModel<Collection<String>> roleNamesModel) {
        super(id, new RolesModel(roleNamesModel));

        add(new AjaxCheckBox("anyRole", new Model(false)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                palette.setEnabled(!getModelObject());
                target.addComponent(palette);
            }
        });
    }

    public RuleRolesFormComponent setHasAnyRole(boolean hasAny) {
        get("anyRole").setDefaultModelObject(hasAny);
        palette.setEnabled(!hasAny);
        return this;
    }

    public boolean isHasAnyRole() {
        return (Boolean) get("anyRole").getDefaultModelObject();
    }

//    
//        add(hasAnyBox);
//        if (hasStoredAnyRole(rootObject)) {
//            rolePalette.setEnabled(false);
//            rolePalette.add(new AttributeAppender("disabled", true, new Model<String>("disabled"), " "));
//            hasAnyBox.setDefaultModelObject(Boolean.TRUE);
//        }
//        else {
//            rolePalette.setEnabled(true);
//            rolePalette.add(new AttributeAppender("enabled", true, new Model<String>("enabled"), " "));
//            hasAnyBox.setDefaultModelObject(Boolean.FALSE);
//        }    
//
//    }
//    
//    public abstract boolean hasStoredAnyRole(T rootObject); 
//    
//    public boolean hasAnyRole() {
//        return (Boolean) hasAnyBox.getDefaultModelObject();
//    }
//    
    public Set<GeoServerRole> getRolesForStoring() {
        Set<GeoServerRole> result = new HashSet<GeoServerRole>();
        if (isHasAnyRole()) {
            result.add(GeoServerRole.ANY_ROLE);
        }
        else { 
            result.addAll(getSelectedRoles());
        }
        return result;
    }

    public Set<String> getRolesNamesForStoring() {
        Set<String> result = new HashSet<String>();
        for (GeoServerRole role : getRolesForStoring()) {
            result.add(role.getAuthority());
        }
        return result;
    }

    static class RolesModel extends LoadableDetachableModel<List<GeoServerRole>>{

        IModel<Collection<String>> roleNamesModel;

        RolesModel(IModel<Collection<String>> roleNamesModel) {
            this.roleNamesModel = roleNamesModel;
        }

        @Override
        protected List<GeoServerRole> load() {
            GeoServerRoleService roleService = GeoServerApplication.get().getSecurityManager()
                .getActiveRoleService();
            List<GeoServerRole> roles = new ArrayList();
            for (String roleName : roleNamesModel.getObject()) {
                try {
                    roles.add(roleService.getRoleByName(roleName));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return roles;
        }

        @Override
        public void setObject(List<GeoServerRole> object) {
            super.setObject(object);

            //set back to the delegate model
            Collection<String> roleNames = roleNamesModel.getObject();
            roleNames.clear();
            
            for (GeoServerRole role : object) {
                roleNames.add(role.getAuthority());
            }
            //roleNamesModel.setObject(roleNames);
        }
    }


//    public boolean isHasAny() {
//        return hasAny;
//    }
//
//    public void setHasAny(boolean hasAny) {
//        this.hasAny = hasAny;
//    }
//
//    @Override
//    public void updateModel() {
//        super.updateModel();
//        hasAnyBox.updateModel();
//    }

}
