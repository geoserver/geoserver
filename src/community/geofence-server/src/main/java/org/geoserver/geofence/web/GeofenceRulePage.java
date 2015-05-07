/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.springframework.dao.DuplicateKeyException;

/**
 * 
 * Internal Geofence Rule Page
 * 
 * @author Niels Charlier
 *
 */
public class GeofenceRulePage extends GeoServerSecuredPage {

    protected DropDownChoice<String> userChoice, roleChoice, serviceChoice, requestChoice, workspaceChoice, layerChoice, accessChoice;
    protected DropDownChoice<GrantType> grantTypeChoice;
    
    public GeofenceRulePage(final ShortRule rule, final GeofenceRulesModel rules) {
        // build the form
        final Form<ShortRule> form = new Form<ShortRule>("form", new CompoundPropertyModel<ShortRule>(rule));
        add(form);
        
        form.add(new TextField<Integer>("priority").setRequired(true));
        
        form.add(roleChoice = new DropDownChoice<String>("roleName", getRoleNames()));
        roleChoice.add(new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = -2880886409750911044L;

			@Override
            protected void onUpdate(AjaxRequestTarget target) {
                userChoice.setChoices(getUserNames(roleChoice.getConvertedInput()));
                ((ShortRule) form.getModelObject()).setUserName(null);
                userChoice.modelChanged();
                target.addComponent(userChoice);
            }
        });
        roleChoice.setNullValid(true);

        form.add(userChoice = new DropDownChoice<String>("userName", getUserNames(rule.getRoleName())));
        userChoice.setOutputMarkupId(true);
        userChoice.setNullValid(true);
                        
        form.add(serviceChoice = new DropDownChoice<String>("service", getServiceNames()));
        serviceChoice.add(new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = -5925784823433092831L;

			@Override
            protected void onUpdate(AjaxRequestTarget target) {
                requestChoice.setChoices(getOperationNames(serviceChoice.getConvertedInput()));
                ((ShortRule) form.getModelObject()).setRequest(null);
                requestChoice.modelChanged();
                target.addComponent(requestChoice);
            }
        });
        serviceChoice.setNullValid(true);
        
        form.add(requestChoice = new DropDownChoice<String>("request", getOperationNames(rule.getService()),
                new CaseConversionRenderer()));
        requestChoice.setOutputMarkupId(true);
        requestChoice.setNullValid(true);
        
        form.add(workspaceChoice = new DropDownChoice<String>("workspace", getWorkspaceNames()));
        workspaceChoice.add(new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = 732177308220189475L;

			@Override
            protected void onUpdate(AjaxRequestTarget target) {
                layerChoice.setChoices(getLayerNames(workspaceChoice.getConvertedInput()));
                ((ShortRule) form.getModelObject()).setLayer(null);
                layerChoice.modelChanged();
                target.addComponent(layerChoice);
            }
        });
        workspaceChoice.setNullValid(true);

        form.add(layerChoice = new DropDownChoice<String>("layer", getLayerNames(rule.getWorkspace())));
        layerChoice.setOutputMarkupId(true);
        layerChoice.setNullValid(true);

        form.add(grantTypeChoice = 
            new DropDownChoice<GrantType>("access", Arrays.asList(GrantType.values()), new GrantTypeRenderer()));        
        grantTypeChoice.setRequired(true);


        // build the submit/cancel
        form.add(new SubmitLink("save") {
			private static final long serialVersionUID = 3735176778941168701L;

			@Override
            public void onSubmit() {
                ShortRule rule = (ShortRule) getForm().getModelObject();
                try {
                    rules.save(rule);
                    doReturn(GeofenceServerPage.class);
                } catch (DuplicateKeyException e) {
                    error(new ResourceModel("GeofenceRulePage.duplicate").getObject());
                } catch (Exception e){
                    error(e);
                }
            }
        });
        form.add(new BookmarkablePageLink<ShortRule>("cancel", GeofenceServerPage.class));
    }


    /**
     * Returns a sorted list of workspace names
     */
    protected List<String> getWorkspaceNames() {
    	
        SortedSet<String> resultSet = new TreeSet<String>();
        for (WorkspaceInfo ws : getCatalog().getFacade().getWorkspaces()) {
            resultSet.add(ws.getName());
        }
        return new ArrayList<String>(resultSet);
    }

    /**
     * Returns a sorted list of layer names in the specified workspace (or * if the workspace is *)
     */
    protected List<String> getLayerNames(String workspaceName) {
        List<String> resultSet = new ArrayList<String>();
        if (workspaceName != null) {
        	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        	
        	try(CloseableIterator<ResourceInfo> it = 
    			getCatalog().getFacade().list(ResourceInfo.class, 
    	            Predicates.equal("store.workspace.name", workspaceName),
    	            null, null, ff.sort("name", SortOrder.ASCENDING)))
    	    {
    	        while(it.hasNext()) {
    	            resultSet.add(it.next().getName());
    	        }
    	    }
        }
    	
        return resultSet;
    }
    
    /**
     * Returns a sorted list of workspace names
     */
    protected List<String> getServiceNames() {
        SortedSet<String> resultSet = new TreeSet<String>();
        for (Service ows : GeoServerExtensions.extensions(Service.class)) {
            resultSet.add(ows.getId().toUpperCase());
        }
        return new ArrayList<String>(resultSet);
    }
    
    /**
     * Returns a sorted list of operation names in the specified service (or * if the workspace is *)
     */
    protected List<String> getOperationNames(String serviceName) {
        SortedSet<String> resultSet = new TreeSet<String>();
        boolean flag = true;
        if (serviceName != null) {
            for (Service ows : GeoServerExtensions.extensions(Service.class)) {
                if (serviceName.equalsIgnoreCase(ows.getId()) && flag) {
                    flag = false;
                    resultSet.addAll(ows.getOperations());
                }
            }
        }
        return new ArrayList<String>(resultSet);
    }
    
    protected List<String> getRoleNames() {
        SortedSet<String> resultSet = new TreeSet<String>();
        try {
            for (GeoServerRole role : securityManager().getRolesForAccessControl()) {
               resultSet.add(role.getAuthority());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        return new ArrayList<String>(resultSet);
    }
    
    protected List<String> getUserNames(String roleName) {
        SortedSet<String> resultSet = new TreeSet<String>();
        
        GeoServerSecurityManager securityManager = securityManager();
        try {
            if (roleName == null) {
                for (String serviceName : securityManager.listUserGroupServices()) {
                    for (GeoServerUser user : securityManager.loadUserGroupService(serviceName).getUsers()) {
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

        return new ArrayList<String>(resultSet);
    }

    /**
     * Makes sure we see translated text, by the raw name is used for the model
     */
    protected class GrantTypeRenderer implements IChoiceRenderer<GrantType> {
		private static final long serialVersionUID = -7478943956804313995L;

		public Object getDisplayValue(GrantType object) {
            return (String) new ParamResourceModel( object.name(), getPage())
                    .getObject();
        }

        public String getIdValue(GrantType object, int index) {
            return object.name();
        }
    }
    
    /**
     * Makes sure that while rendered in mixed case, is stored in uppercase
     */
    protected class CaseConversionRenderer implements IChoiceRenderer<String> {
		private static final long serialVersionUID = 4238195087731806209L;

		public Object getDisplayValue(String object) {
            return object;
        }

        public String getIdValue(String object, int index) {
            return object.toUpperCase();
        }
    }
    
    protected GeoServerSecurityManager securityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }
    
    
}
