/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.WPSInfo;

/**
 * Configure the WPS service global informations
 *
 * @author Andrea Aime - GeoSolutions
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
        TextField<Integer> connectionTimeout =
                new TextField<Integer>("connectionTimeout", Integer.class);
        connectionTimeout.add(RangeValidator.minimum(-1));
        form.add(connectionTimeout);

        TextField<Integer> maxSynchProcesses =
                new TextField<Integer>("maxSynchronousProcesses", Integer.class);
        maxSynchProcesses.add(RangeValidator.minimum(1));
        form.add(maxSynchProcesses);

        TextField<Integer> maxSynchExecutionTime =
                new TextField<Integer>("maxSynchronousExecutionTime", Integer.class);
        maxSynchExecutionTime.add(RangeValidator.minimum(-1));
        form.add(maxSynchExecutionTime);

        TextField<Integer> maxSynchTotalTime =
                new TextField<Integer>("maxSynchronousTotalTime", Integer.class);
        maxSynchTotalTime.add(RangeValidator.minimum(-1));
        form.add(maxSynchTotalTime);

        TextField<Integer> maxAsynchProcesses =
                new TextField<Integer>("maxAsynchronousProcesses", Integer.class);
        maxAsynchProcesses.add(RangeValidator.minimum(1));
        form.add(maxAsynchProcesses);

        TextField<Integer> maxAsynchExecutionTime =
                new TextField<Integer>("maxAsynchronousExecutionTime", Integer.class);
        maxAsynchExecutionTime.add(RangeValidator.minimum(-1));
        form.add(maxAsynchExecutionTime);

        TextField<Integer> maxAsynchTotalTime =
                new TextField<Integer>("maxAsynchronousTotalTime", Integer.class);
        maxAsynchTotalTime.add(RangeValidator.minimum(-1));
        form.add(maxAsynchTotalTime);

        TextField<Integer> resourceExpirationTimeout =
                new TextField<Integer>("resourceExpirationTimeout", Integer.class);
        resourceExpirationTimeout.add(RangeValidator.minimum(0));
        form.add(resourceExpirationTimeout);

        // GeoServerFileChooser chooser = new GeoServerFileChooser("storageDirectory",
        // new PropertyModel<String>(info, "storageDirectory"));
        DirectoryParamPanel chooser =
                new DirectoryParamPanel(
                        "storageDirectory",
                        new PropertyModel<String>(info, "storageDirectory"),
                        new ParamResourceModel("storageDirectory", this),
                        false);
        form.add(chooser);

        form.add(new TotalTimeValidator(maxSynchTotalTime, maxSynchExecutionTime));
        form.add(new TotalTimeValidator(maxAsynchTotalTime, maxAsynchExecutionTime));
    }

    @Override
    protected void handleSubmit(WPSInfo info) {
        super.handleSubmit(info);
    }

    /** Validator that checks that the total time is greater than the execution time */
    class TotalTimeValidator extends AbstractFormValidator {

        private static final long serialVersionUID = 1L;

        private FormComponent<Integer> totalTime;
        private FormComponent<Integer> executionTime;

        public TotalTimeValidator(
                FormComponent<Integer> totalTime, FormComponent<Integer> executionTime) {
            this.totalTime = totalTime;
            this.executionTime = executionTime;
        }

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {totalTime, executionTime};
        }

        @Override
        public void validate(Form<?> form) {
            if (executionTime.getConvertedInput() != null
                    && totalTime.getConvertedInput() != null
                    && totalTime.getConvertedInput() != 0
                    && totalTime.getConvertedInput() < executionTime.getConvertedInput()) {
                form.error(new ParamResourceModel("totalTimeError", getPage()).getString());
            }
        }
    }
}
