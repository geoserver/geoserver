/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.WPSInfo;

/**
 * Configure the WPS service global informations
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class WPSAdminPage extends BaseServiceAdminPage<WPSInfo> {

    private List<ProcessGroupInfo> processFactories;

    public WPSAdminPage() {
        super();
    }
    
    public WPSAdminPage(WPSInfo service) {
        super(service);
    }

    public WPSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    protected Class<WPSInfo> getServiceClass() {
        return WPSInfo.class;
    }

    protected String getServiceName() {
        return "WPS";
    }

    @Override
    protected void build(IModel info, final Form form) {
        TextField connectionTimeout = new TextField("connectionTimeout", Integer.class);
        connectionTimeout.add(new MinimumValidator<Integer>(-1));
        form.add(connectionTimeout);
        
        TextField maxSynchProcesses = new TextField("maxSynchronousProcesses", Integer.class);
        maxSynchProcesses.add(new MinimumValidator<Integer>(1));
        form.add(maxSynchProcesses);
        
        TextField maxAsynchProcesses = new TextField("maxAsynchronousProcesses", Integer.class);
        maxAsynchProcesses.add(new MinimumValidator<Integer>(1));
        form.add(maxAsynchProcesses);
        
        TextField resourceExpirationTimeout = new TextField("resourceExpirationTimeout", Integer.class);
        resourceExpirationTimeout.add(new MinimumValidator<Integer>(1));
        form.add(resourceExpirationTimeout);
        
        // GeoServerFileChooser chooser = new GeoServerFileChooser("storageDirectory",
        // new PropertyModel<String>(info, "storageDirectory"));
        DirectoryParamPanel chooser = new DirectoryParamPanel("storageDirectory",
                new PropertyModel<String>(
                info, "storageDirectory"), new ParamResourceModel("storageDirectory", this), false);
        form.add(chooser);

        WPSInfo wpsInfo = (WPSInfo) info.getObject();
        processFactories = cloneFactoryInfos(wpsInfo.getProcessGroups());
        ProcessFactoryInfoProvider provider = new ProcessFactoryInfoProvider(processFactories, getLocale());
        GeoServerTablePanel<ProcessGroupInfo> processFilterEditor = new GeoServerTablePanel<ProcessGroupInfo>("processFilterTable", provider) {

            @Override
            protected Component getComponentForProperty(String id, final IModel itemModel,
                    Property<ProcessGroupInfo> property) {
                
                if(property.getName().equals("enabled")) {
                    Fragment fragment = new Fragment(id, "enabledFragment", WPSAdminPage.this);
                    CheckBox enabled = new CheckBox("enabled", property.getModel(itemModel));
                    enabled.setOutputMarkupId(true);
                    fragment.add(enabled);
                    return fragment;
                } else if(property.getName().equals("prefix")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("title")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("summary")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("edit")) {
                    Fragment fragment = new Fragment(id, "linkFragment", WPSAdminPage.this);
                    // we use a submit link to avoid losing the other edits in the form
                    SubmitLink link = new SubmitLink("link") {

                        @Override
                        public void onSubmit() {
                            ProcessGroupInfo pfi = (ProcessGroupInfo) itemModel.getObject();
                            setResponsePage(new ProcessSelectionPage(WPSAdminPage.this, pfi));
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
    }
    
    private List<ProcessGroupInfo> cloneFactoryInfos(List<ProcessGroupInfo> processFactories) {
        List<ProcessGroupInfo> result = new ArrayList<ProcessGroupInfo>();
        for (ProcessGroupInfo pfi : processFactories) {
            result.add(pfi.clone());
        }
        
        return result;
    }

    @Override
    protected void handleSubmit(WPSInfo info) {
        // overwrite the process factories that we did clone to achieve isolation
        List<ProcessGroupInfo> factories = info.getProcessGroups();
        factories.clear();
        factories.addAll(processFactories);
        super.handleSubmit(info);
    }

}
