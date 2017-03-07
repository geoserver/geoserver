/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.WPSInfo;

/**
 * A page listing all WPS groups, 
 * allowing enable/disable single groups and add/remove roles to grant access to all its processes
 * This page links to WPS service security configuration page to works on single processes.
 * 
 * @see ProcessSelectionPage
 */

@SuppressWarnings("serial")
public class WPSAccessRulePage extends AbstractSecurityPage {

    static final List<CatalogMode> CATALOG_MODES = Arrays.asList(CatalogMode.HIDE, CatalogMode.MIXED, CatalogMode.CHALLENGE);
    
    private List<ProcessGroupInfo> processFactories;
    private WPSInfo wpsInfo;
    private RadioChoice catalogModeChoice;

    private List<String> availableRoles = new ArrayList<String>();

    public WPSAccessRulePage() {
        wpsInfo = getGeoServer().getService( WPSInfo.class );
        Form form = new Form("form", new CompoundPropertyModel(wpsInfo));

        processFactories = cloneFactoryInfos(wpsInfo.getProcessGroups());
        ProcessFactoryInfoProvider provider = new ProcessFactoryInfoProvider(processFactories, getLocale());
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        try {
            for(GeoServerRole r : roleService.getRoles()){
                availableRoles.add(r.getAuthority());
            }            
        } catch (IOException e1) {
            LOGGER.log(Level.FINER, e1.getMessage(), e1);
        }

        TextField<Integer> maxComplexInputSize = new TextField<Integer>("maxComplexInputSize",
                Integer.class);
        maxComplexInputSize.add(RangeValidator.minimum(0));
        form.add(maxComplexInputSize);

        final AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(false);
        settings.setShowListOnEmptyInput(true);
        settings.setShowListOnFocusGain(true);
        settings.setMaxHeightInPx(100);
        
        GeoServerTablePanel<ProcessGroupInfo> processFilterEditor = new GeoServerTablePanel<ProcessGroupInfo>("processFilterTable", provider) {

            @Override
            protected Component getComponentForProperty(String id, final IModel<ProcessGroupInfo> itemModel,
                    Property<ProcessGroupInfo> property) {

                if(property.getName().equals("enabled")) {
                    Fragment fragment = new Fragment(id, "enabledFragment", WPSAccessRulePage.this);
                    CheckBox enabled = new CheckBox("enabled", (IModel<Boolean>) property.getModel(itemModel));
                    enabled.setOutputMarkupId(true);
                    fragment.add(enabled);
                    return fragment;
                } else if(property.getName().equals("prefix")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("title")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("summary")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("roles")) {
                    Fragment fragment = new Fragment(id, "rolesFragment", WPSAccessRulePage.this);
                    TextArea<String> roles = new  TextArea<String>("roles", (IModel<String>) property.getModel(itemModel)) {
                        public <C extends Object> org.apache.wicket.util.convert.IConverter<C> getConverter(java.lang.Class<C> type) {
                            return new RolesConverter(availableRoles);
                        };
                    };
                    StringBuilder selectedRoles = new StringBuilder ();
                    IAutoCompleteRenderer<String> roleRenderer = new RolesRenderer(selectedRoles);
                    AutoCompleteBehavior<String> b = new RolesAutoCompleteBehavior(roleRenderer,settings,selectedRoles,availableRoles);
                    roles.setOutputMarkupId(true);
                    roles.add(b);
                    fragment.add(roles);
                    return fragment;   
                } else if(property.getName().equals("edit")) {
                    Fragment fragment = new Fragment(id, "linkFragment", WPSAccessRulePage.this);
                    // we use a submit link to avoid losing the other edits in the form
                    Link link = new Link("link") {
                        @Override
                        public void onClick() {
                            ProcessGroupInfo pfi = (ProcessGroupInfo) itemModel.getObject();
                            setResponsePage(new ProcessSelectionPage(WPSAccessRulePage.this, pfi));
                        }
                    };   
                    fragment.add(link);

                    return fragment;
                }

                return null;
            }
        };
        processFilterEditor.setFilterable(false);
        processFilterEditor.setPageable(false);
        processFilterEditor.setOutputMarkupId( true );
        form.add(processFilterEditor);  
        
        form.add(new AjaxLink("processAccessModeHelp") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.showInfo(target, 
                    new StringResourceModel("processAccessModeHelp.title",getPage(), null),
                    new StringResourceModel("processAccessModeHelp.message",getPage(), null));
            }
        });
        catalogModeChoice = new RadioChoice("processAccessMode", new PropertyModel<CatalogMode>(wpsInfo, "catalogMode"), CATALOG_MODES, new CatalogModeRenderer());
        catalogModeChoice.setSuffix(" ");
        form.add(catalogModeChoice);

        SubmitLink submit = new SubmitLink("submit",new StringResourceModel( "save", (Component)null, null) ) {
            @Override
            public void onSubmit() {
                try {
                    // overwrite the process factories that we did clone to achieve isolation
                    List<ProcessGroupInfo> factories = wpsInfo.getProcessGroups();
                    factories.clear();
                    factories.addAll(processFactories);
                    getGeoServer().save(wpsInfo);
                    doReturn();
                }catch(Exception e) {
                    error(e);
                }
            }
        };
        form.add(submit);

        Button cancel = new Button( "cancel" ) {
            public void onSubmit() {
                doReturn();
            }
        };
        form.add( cancel );

        add(form);

    }

    private List<ProcessGroupInfo> cloneFactoryInfos(List<ProcessGroupInfo> processFactories) {
        List<ProcessGroupInfo> result = new ArrayList<ProcessGroupInfo>();
        for (ProcessGroupInfo pfi : processFactories) {
            result.add(pfi.clone());
        }

        return result;
    }
    
    class CatalogModeRenderer extends ChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new ParamResourceModel(((CatalogMode) object).name(), getPage())
                    .getObject();
        }

        public String getIdValue(Object object, int index) {
            return ((CatalogMode) object).name();
        }
    }

}
