/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.wicket.GeoServerDialog;

/**
 * A label, a text field, a directory chooser.
 *
 * @author Andrea Aime
 */
public class DirectoryInput extends FileInput {

    private static final long serialVersionUID = -8317791966175845831L;
    protected GeoServerDialog gsDialog;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    @SafeVarargs
    public DirectoryInput(
            final String id,
            final IModel<String> paramValue,
            final IModel<String> paramLabelModel,
            final boolean required,
            IValidator<? super String>... validators) {
        super(id, paramValue, paramLabelModel, required, validators);

        // override the dialog component
        remove(dialog);
        add(gsDialog = new GeoServerDialog("dialog"));
    }

    @Override
    protected Component chooserButton(final String windowTitle) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {

                    private static final long serialVersionUID = -2860146532287292092L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        gsDialog.setTitle(new Model<String>(windowTitle));
                        gsDialog.showOkCancel(
                                target,
                                new GeoServerDialog.DialogDelegate() {

                                    /** */
                                    private static final long serialVersionUID =
                                            1576266249751904398L;

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        GeoServerFileChooser chooser =
                                                (GeoServerFileChooser) contents;
                                        String path =
                                                ((File) chooser.getDefaultModelObject())
                                                        .getAbsolutePath();
                                        // clear the raw input of the field won't show the new model
                                        // value
                                        textField.clearInput();
                                        textField.setModelValue(new String[] {path});

                                        target.add(textField);
                                        return true;
                                    }

                                    @Override
                                    public void onClose(AjaxRequestTarget target) {
                                        // update the field with the user chosen value
                                        target.add(textField);
                                    }

                                    @Override
                                    protected Component getContents(String id) {
                                        File file = null;
                                        textField.processInput();
                                        String input = (String) textField.getConvertedInput();
                                        if (input != null && !input.equals("")) {
                                            file = new File(input);
                                        }

                                        GeoServerFileChooser chooser =
                                                new GeoServerFileChooser(id, new Model<File>(file));
                                        chooser.setFilter(fileFilter);

                                        return chooser;
                                    }
                                });
                    }
                };
        return link;
    }
}
