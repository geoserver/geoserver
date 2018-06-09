/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.io.File;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

@SuppressWarnings("serial")
public class ResourceFilePanel extends Panel {

    private static final String[] FILE_EXTENSIONS =
            new String[] {".zip", ".gz", ".tar", ".tgz", ".bz"};

    String file;

    TextField fileField;
    GeoServerDialog dialog;

    public ResourceFilePanel(String id) {
        super(id);

        add(dialog = new GeoServerDialog("dialog"));

        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);

        fileField = new TextField("file");
        fileField.setRequired(true);
        fileField.setOutputMarkupId(true);
        fileField.add(
                new OnChangeAjaxBehavior() {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        // Access the updated model value:
                        final String valueAsString =
                                ((TextField<String>) getComponent()).getModelObject();

                        // use what the user currently typed
                        File file = null;
                        if (!valueAsString.trim().equals("")) {
                            file = new File(valueAsString);
                            if (!file.exists()) file = null;
                        }
                    }
                });

        form.add(fileField);
        form.add(chooserButton(form));
    }

    public Resource getResource() {
        return Files.asResource(new File(this.file));
    };

    Component chooserButton(Form form) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        dialog.setTitle(new ParamResourceModel("chooseFile", this));
                        dialog.showOkCancel(
                                target,
                                new GeoServerDialog.DialogDelegate() {

                                    @Override
                                    protected Component getContents(String id) {
                                        // use what the user currently typed
                                        File file = null;
                                        if (!fileField.getInput().trim().equals("")) {
                                            file = new File(fileField.getInput());
                                            if (!file.exists()) file = null;
                                        }

                                        GeoServerFileChooser chooser =
                                                new GeoServerFileChooser(id, new Model(file)) {
                                                    @Override
                                                    protected void fileClicked(
                                                            File file, AjaxRequestTarget target) {
                                                        ResourceFilePanel.this.file =
                                                                file.getAbsolutePath();

                                                        fileField.clearInput();
                                                        fileField.setModelObject(
                                                                file.getAbsolutePath());

                                                        target.add(fileField);
                                                        dialog.close(target);
                                                    }
                                                };

                                        initFileChooser(chooser);
                                        return chooser;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        GeoServerFileChooser chooser =
                                                (GeoServerFileChooser) contents;
                                        file =
                                                ((File) chooser.getDefaultModelObject())
                                                        .getAbsolutePath();

                                        // clear the raw input of the field won't show the new model
                                        // value
                                        fileField.clearInput();
                                        // fileField.setModelObject(file);

                                        target.add(fileField);
                                        return true;
                                    }

                                    @Override
                                    public void onClose(AjaxRequestTarget target) {
                                        // update the field with the user chosen value
                                        target.add(fileField);
                                    }
                                });
                    }
                };
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);
        return link;
    }

    SubmitLink submitLink() {
        return new SubmitLink("submit") {

            @Override
            public void onSubmit() {}
        };
    }

    protected void initFileChooser(GeoServerFileChooser fileChooser) {
        fileChooser.setFilter(new Model(new ExtensionFileFilter(FILE_EXTENSIONS)));
        // fileChooser.setFilter(new Model((Serializable)FileFileFilter.FILE));
    }
}
