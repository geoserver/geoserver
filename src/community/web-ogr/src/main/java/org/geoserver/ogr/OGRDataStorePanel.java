/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogr;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.browser.DirectoryInput;
import org.geoserver.web.wicket.browser.FileInput;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.geotools.data.ogr.jni.JniOGRDataStoreFactory;

/** Custom data store panel for OGR data stores */
public class OGRDataStorePanel extends DefaultDataStoreEditPanel {

    /**
     * Creates a new parameters panel with a list of input fields matching the {@link Param}s for
     * the factory related to the {@code DataStoreInfo} that's the model of the provided {@code
     * Form}.
     *
     * @param componentId the id for this component instance
     * @param storeEditForm the form being build by the calling class, whose model is the {@link
     *     DataStoreInfo} being edited
     */
    public OGRDataStorePanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);
    }

    @Override
    protected Panel getInputComponent(
            String componentId, IModel paramsModel, ParamInfo paramMetadata) {
        String paramName = paramMetadata.getName();
        IModel<String> labelModel = new ResourceModel(paramName, paramName);

        // show a dropdown using the available driver names
        if (OGRDataStoreFactory.OGR_DRIVER_NAME.key.equals(paramName)) {
            List<String> drivers =
                    new ArrayList<>(new JniOGRDataStoreFactory().getAvailableDrivers());
            Collections.sort(drivers, String.CASE_INSENSITIVE_ORDER);

            IModel<Serializable> valueModel = new MapModel(paramsModel, paramName);
            return new DropDownChoiceParamPanel(
                    componentId, valueModel, labelModel, drivers, false);
        }

        // show a file entry, but allow for random strings to be entered as well
        if (OGRDataStoreFactory.OGR_NAME.key.equals(paramName)) {
            return new FileOrDirectoryParamPanel(
                    componentId, new MapModel(paramsModel, paramName), labelModel, true) {};
        }

        return super.getInputComponent(componentId, paramsModel, paramMetadata);
    }

    /** Delegate that allows both files and directories to be chosen */
    class FileOrDirectoryParamPanel extends DirectoryParamPanel {

        public FileOrDirectoryParamPanel(
                String id,
                IModel<String> paramValue,
                IModel<String> paramLabelModel,
                boolean required,
                IValidator<? super String>... validators) {
            super(id, paramValue, paramLabelModel, required, validators);
        }

        @Override
        protected FileInput getFilePathInput(
                IModel<String> paramValue,
                IModel<String> paramLabelModel,
                boolean required,
                IValidator<? super String>[] validators) {
            // the file chooser
            return new DirectoryInput(
                    "fileInput", paramValue, paramLabelModel, required, validators) {
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
                                    gsDialog.showOkCancel(target, new OGRDialogDelegate());
                                }
                            };
                    return link;
                }

                @Override
                protected IModel<String> getFileModel(IModel<String> paramValue) {
                    // avoid the "magic" transformations of paths, as the input might not
                    // be a path at all
                    return paramValue;
                }

                class OGRDialogDelegate extends GeoServerDialog.DialogDelegate {

                    /** */
                    private static final long serialVersionUID = 1576266249751904398L;

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        GeoServerFileChooser chooser = (GeoServerFileChooser) contents;
                        String path = ((File) chooser.getDefaultModelObject()).getAbsolutePath();
                        // clear the raw input of the field won't show the new
                        // model
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
                        String input = textField.getConvertedInput();
                        if (input != null && !input.equals("")) {
                            file = new File(input);
                        }

                        GeoServerFileChooser chooser =
                                new GeoServerFileChooser(id, new Model<File>(file)) {
                                    protected void fileClicked(
                                            File file, AjaxRequestTarget target) {
                                        // clear the raw input of the field
                                        // won't show the new model
                                        // value
                                        textField.clearInput();
                                        textField.setModelObject(file.getAbsolutePath());

                                        target.add(textField);
                                        dialog.close(target);
                                    };
                                };
                        chooser.setFilter(fileFilter);

                        return chooser;
                    }
                }
            };
        }
    }
}
