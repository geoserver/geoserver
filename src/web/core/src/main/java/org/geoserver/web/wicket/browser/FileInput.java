/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.data.store.panel.FileModel;
import org.geotools.util.logging.Logging;

/**
 * A text field plus file chooser, to be used in any form
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileInput extends Panel {
    private static final Logger LOGGER = Logging.getLogger(FileInput.class);
    protected TextField<String> textField;
    protected ModalWindow dialog;
    protected IModel<? extends FileFilter> fileFilter;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    @SafeVarargs
    public FileInput(
            final String id,
            final IModel<String> paramValue,
            final IModel<String> paramLabelModel,
            final boolean required,
            IValidator<? super String>... validators) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, paramValue);

        // add the dialog for the file chooser
        add(dialog = new ModalWindow("dialog"));

        // the text field, with a decorator for validations
        FileRootsFinder rootsFinder = new FileRootsFinder(false);
        textField =
                new AutoCompleteTextField<String>("paramValue", getFileModel(paramValue)) {
                    @Override
                    protected Iterator<String> getChoices(String input) {
                        try {
                            // do we need to filter files?
                            FileFilter fileFilter =
                                    FileInput.this.fileFilter != null
                                            ? FileInput.this.fileFilter.getObject()
                                            : null;

                            return rootsFinder.getMatches(input, fileFilter).iterator();
                        } catch (Exception e) {
                            // this is a helper, don't let it break the UI at runtime but log errors
                            // instead
                            LOGGER.log(
                                    Level.INFO,
                                    "Failed to provide autocomplete for path " + input,
                                    e);
                            return Collections.emptyIterator();
                        }
                    }
                };
        textField.setRequired(required);
        textField.setOutputMarkupId(true);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        textField.setLabel(paramLabelModel);

        if (validators != null) {
            for (IValidator<? super String> validator : validators) {
                textField.add(validator);
            }
        }

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(textField);
        feedback.add(chooserButton(paramLabelModel.getObject()));
        add(feedback);
    }

    protected IModel<String> getFileModel(IModel<String> paramValue) {
        return new FileModel(paramValue);
    }

    protected Component chooserButton(final String windowTitle) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {

                    private static final long serialVersionUID = -6640131658256808053L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        File file = null;
                        textField.processInput();
                        String input = textField.getConvertedInput();
                        if (input != null && !input.equals("")) {
                            file = new File(input);
                        }

                        GeoServerFileChooser chooser =
                                new GeoServerFileChooser(
                                        dialog.getContentId(), new Model<File>(file)) {
                                    private static final long serialVersionUID =
                                            -7096642192491726498L;

                                    protected void fileClicked(
                                            File file, AjaxRequestTarget target) {
                                        // clear the raw input of the field won't show the new model
                                        // value
                                        textField.clearInput();
                                        textField.setModelObject(file.getAbsolutePath());

                                        target.add(textField);
                                        dialog.close(target);
                                    };
                                };
                        chooser.setFileTableHeight(null);
                        chooser.setFilter(fileFilter);
                        dialog.setContent(chooser);
                        dialog.setTitle(windowTitle);
                        dialog.show(target);
                    }
                };
        return link;
    }

    /** The text field stored inside the panel. */
    public FormComponent<String> getFormComponent() {
        return textField;
    }

    /** Sets the filter that will act in the file chooser dialog */
    public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
        this.fileFilter = fileFilter;
    }
}
