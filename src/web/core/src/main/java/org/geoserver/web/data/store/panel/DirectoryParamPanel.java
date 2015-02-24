/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.io.File;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

/**
 * A label, a text field, a directory chooser.
 * 
 * @author Andrea Aime
 */
@SuppressWarnings("serial")
public class DirectoryParamPanel extends FileParamPanel {

    GeoServerDialog gsDialog;

    /**
     * 
     * @param id
     * @param paramsMap
     * @param paramName
     * @param paramLabelModel
     * @param required
     * @param validators
     *            any extra validator that should be added to the input field, or {@code null}
     */
    public DirectoryParamPanel(final String id, final IModel paramValue,
            final IModel paramLabelModel, final boolean required, IValidator... validators) {
        super(id, paramValue, paramLabelModel, required, validators);

        // override the dialog component
        remove(dialog);
        add(gsDialog = new GeoServerDialog("dialog"));
    }

    @Override
    protected Component chooserButton(final String windowTitle) {
        AjaxSubmitLink link = new AjaxSubmitLink("chooser") {

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                gsDialog.setTitle(new Model(windowTitle));
                gsDialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        GeoServerFileChooser chooser = (GeoServerFileChooser) contents;
                        String path = ((File) chooser.getDefaultModelObject()).getAbsolutePath();
                        // clear the raw input of the field won't show the new model value
                        textField.clearInput();
                        textField.setModelValue(new String[] { path });

                        target.addComponent(textField);
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // update the field with the user chosen value
                        target.addComponent(textField);
                    }

                    @Override
                    protected Component getContents(String id) {
                        File file = null;
                        textField.processInput();
                        String input = (String) textField.getConvertedInput();
                        if (input != null && !input.equals("")) {
                            file = new File(input);
                        }

                        GeoServerFileChooser chooser = new GeoServerFileChooser(id, new Model(file));
                        chooser.setFilter(fileFilter);

                        return chooser;
                    }
                });
            }

        };
        return link;
    }

}
