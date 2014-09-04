/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.web.FilteredProcessesProvider.FilteredProcess;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

public class ProcessSelectionPage extends GeoServerSecuredPage {
    
    private String title;
    private GeoServerTablePanel<FilteredProcess> processSelector;
    private ProcessGroupInfo pfi;

    public ProcessSelectionPage(final WPSAdminPage origin, final ProcessGroupInfo pfi) {
        this.pfi = pfi;
        
        // prepare the process factory title
        Class<? extends ProcessFactory> factoryClass = pfi.getFactoryClass();
        ProcessFactory pf = GeoServerProcessors.getProcessFactory(factoryClass, false);
        if(pf == null) {
            throw new IllegalArgumentException("Failed to locate the process factory " + factoryClass);
        }
        this.title = pf.getTitle().toString(getLocale());
        
        Form form = new Form("form");
        add(form);
        
        final FilteredProcessesProvider provider = new FilteredProcessesProvider(pfi, getLocale());
        processSelector = new GeoServerTablePanel<FilteredProcess>("selectionTable", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, final IModel itemModel,
                    Property<FilteredProcess> property) {
                
                if(property.getName().equals("title")) {
                    return new Label(id, property.getModel(itemModel));
                } else if(property.getName().equals("description")) {
                    return new Label(id, property.getModel(itemModel));
                }                
                return null;
            }
        };
        processSelector.setFilterable(false);
        processSelector.setPageable(false);
        processSelector.setOutputMarkupId( true );
        List<FilteredProcess> processes = provider.getItems();
        boolean allSelected = true;
        for (int i = 0; i < processes.size(); i++) {
            FilteredProcess process = processes.get(i);
            if(!pfi.getFilteredProcesses().contains(process.getName())) {
                processSelector.selectIndex(i);
            } else {
                allSelected = false;
            }
        }
        if(allSelected) {
            processSelector.selectAll();
        }
        form.add(processSelector);
        
        SubmitLink apply = new SubmitLink("apply") {
          @Override
            public void onSubmit() {
                super.onSubmit();
                
                pfi.getFilteredProcesses().clear();
                pfi.getFilteredProcesses().addAll(getFilteredProcesses());
                setResponsePage(origin);
            }  
        };
        form.add(apply);
        Link cancel = new Link("cancel") {
            @Override
            public void onClick() {
                setResponsePage(origin);
            }
        };
        form.add(cancel);
    }
    
    protected Collection<? extends Name> getFilteredProcesses() {
        ProcessFactory pf = GeoServerProcessors.getProcessFactory(pfi.getFactoryClass(), false);
        List<Name> disabled = new ArrayList<Name>(pf.getNames());
        for(FilteredProcess fp : processSelector.getSelection()) {
            disabled.remove(fp.getName());
        }
        
        return disabled;
    }

    @Override
    protected String getDescription() {
        return new ParamResourceModel("description", this, title).getString();
    }

}
