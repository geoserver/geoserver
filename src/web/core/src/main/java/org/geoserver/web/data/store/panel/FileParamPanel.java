/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.io.FileFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.wicket.browser.FileInput;

/**
 * A label, a text field, a file chooser
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileParamPanel extends Panel implements ParamPanel {
    private static final long serialVersionUID = 2630421795437249103L;
    private final FileInput fileInput;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    @SafeVarargs
    public FileParamPanel(
            final String id,
            final IModel<String> paramValue,
            final IModel<String> paramLabelModel,
            final boolean required,
            IValidator<? super String>... validators) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, paramValue);

        // the label
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);
        this.fileInput = getFilePathInput(paramValue, paramLabelModel, required, validators);
        add(fileInput);
    }

    protected FileInput getFilePathInput(
            IModel<String> paramValue,
            IModel<String> paramLabelModel,
            boolean required,
            IValidator<? super String>[] validators) {
        // the file chooser
        return new FileInput("fileInput", paramValue, paramLabelModel, required, validators);
    }

    /** The text field stored inside the panel. */
    public FormComponent<String> getFormComponent() {
        return fileInput.getFormComponent();
    }

    /** Sets the filter that will act in the file chooser dialog */
    public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
        this.fileInput.setFileFilter(fileFilter);
    }
}
