/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.cog.panel;

import java.io.FileFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.data.store.panel.ParamPanel;

/** Panel for Cog URL. */
public class CogUrlParamPanel extends Panel implements ParamPanel {
    CogInput cogInput;

    @SafeVarargs
    public CogUrlParamPanel(
            final String id,
            final IModel<String> paramValue,
            final IModel<String> paramLabelModel,
            final IModel<Boolean> controlFlag,
            final boolean required,
            IValidator<? super String>... validators) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, paramValue);

        // the label
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);
        this.cogInput = getCogInput(paramValue, paramLabelModel, controlFlag, required, validators);
        add(cogInput);
    }

    protected CogInput getCogInput(
            IModel<String> paramValue,
            IModel<String> paramLabelModel,
            IModel<Boolean> controlFlag,
            boolean required,
            IValidator<? super String>[] validators) {
        return new CogInput(
                "cogInput", paramValue, paramLabelModel, controlFlag, required, validators);
    }

    /** The text field stored inside the panel. */
    @Override
    public FormComponent<String> getFormComponent() {
        return cogInput.getFormComponent();
    }

    /** Sets the filter that will act in the file chooser dialog */
    public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
        this.cogInput.setFileFilter(fileFilter);
    }
}
