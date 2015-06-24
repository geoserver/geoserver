/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.WPSInfo;

/**
 * Configure the WPS service global informations
 *
 * @author Andrea Aime - GeoSolutions
 *
 */
public class WPSAdminPage extends BaseServiceAdminPage<WPSInfo> {

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

        TextField maxSynchExecutionTime = new TextField("maxSynchronousExecutionTime",
                Integer.class);
        maxSynchExecutionTime.add(new MinimumValidator<Integer>(-1));
        form.add(maxSynchExecutionTime);

        TextField maxAsynchProcesses = new TextField("maxAsynchronousProcesses", Integer.class);

        maxAsynchProcesses.add(new MinimumValidator<Integer>(1));
        form.add(maxAsynchProcesses);

        TextField maxAsynchExecutionTime = new TextField("maxAsynchronousExecutionTime",
                Integer.class);
        maxAsynchExecutionTime.add(new MinimumValidator<Integer>(-1));
        form.add(maxAsynchExecutionTime);

        TextField resourceExpirationTimeout = new TextField("resourceExpirationTimeout", Integer.class);
        resourceExpirationTimeout.add(new MinimumValidator<Integer>(1));
        form.add(resourceExpirationTimeout);

        form.add(new CheckBox("synchronousDisabled"));

        // GeoServerFileChooser chooser = new GeoServerFileChooser("storageDirectory",
        // new PropertyModel<String>(info, "storageDirectory"));
        DirectoryParamPanel chooser = new DirectoryParamPanel("storageDirectory",
                new PropertyModel<String>(
                info, "storageDirectory"), new ParamResourceModel("storageDirectory", this), false);
        form.add(chooser);
    }

    @Override
    protected void handleSubmit(WPSInfo info) {
        super.handleSubmit(info);
    }

}
