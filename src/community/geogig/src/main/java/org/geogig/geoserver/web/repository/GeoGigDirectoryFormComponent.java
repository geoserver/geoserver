/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.File;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.FileParamPanel;

/**
 * A panel to browse the filesystem for geogig repositories.
 *
 * <p>Adapted from {@link FileParamPanel}
 */
class GeoGigDirectoryFormComponent extends FormComponentPanel<String> {

    private static final long serialVersionUID = -7456670856888745195L;

    private final TextField<String> directory;

    private final ModalWindow dialog;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    GeoGigDirectoryFormComponent(final String id, final IModel<String> valueModel) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, valueModel);

        // add the dialog for the file chooser
        add(dialog = new ModalWindow("dialog"));

        // the text field, with a decorator for validations
        directory = new TextField<>("value", valueModel);
        directory.setRequired(true);
        directory.setOutputMarkupId(true);

        IModel<String> labelModel =
                new ResourceModel("GeoGigDirectoryFormComponent.directory", "Parent directory") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        String value = super.getObject();
                        return value + " *";
                    }
                };

        final Label directoryLabel = new Label("directoryLabel", labelModel.getObject());
        add(directoryLabel);

        directory.setLabel(labelModel);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("wrapper");
        feedback.add(directory);
        feedback.add(chooserButton());
        add(feedback);
    }

    @Override
    public void convertInput() {
        String uri = directory.getConvertedInput();
        setConvertedInput(uri);
    }

    private Component chooserButton() {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        File file = null;
                        directory.processInput();
                        String input = directory.getConvertedInput();
                        if (input != null && !input.isEmpty()) {
                            file = new File(input);
                        }

                        final boolean makeRepositoriesSelectable = false;
                        DirectoryChooser chooser =
                                new DirectoryChooser(
                                        dialog.getContentId(),
                                        new Model<>(file),
                                        makeRepositoriesSelectable) {

                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void geogigDirectoryClicked(
                                            final File file, AjaxRequestTarget target) {
                                        // clear the raw input of the field won't show the new model
                                        // value
                                        directory.clearInput();
                                        directory.setModelObject(file.getAbsolutePath());

                                        target.add(directory);
                                        dialog.close(target);
                                    };

                                    @Override
                                    protected void directorySelected(
                                            File file, AjaxRequestTarget target) {
                                        directory.clearInput();
                                        directory.setModelObject(file.getAbsolutePath());
                                        target.add(directory);
                                        dialog.close(target);
                                    }
                                };
                        chooser.setFileTableHeight(null);
                        dialog.setContent(chooser);
                        dialog.setTitle(
                                new ResourceModel(
                                        "GeoGigDirectoryFormComponent.chooser.chooseParentTile"));
                        dialog.show(target);
                    }
                };
        return link;
    }
}
