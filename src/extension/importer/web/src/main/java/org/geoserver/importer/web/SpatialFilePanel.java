/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

// TODO WICKET8 - Verify this page works OK
public class SpatialFilePanel extends ImportSourcePanel {

    String file;

    TextField<String> fileField;
    GeoServerDialog dialog;

    public SpatialFilePanel(String id) {
        super(id);

        add(dialog = new GeoServerDialog("dialog"));

        Form<SpatialFilePanel> form = new Form<>("form", new CompoundPropertyModel<>(this));
        add(form);

        fileField = new TextField<>("file");
        fileField.setRequired(true);
        fileField.setOutputMarkupId(true);
        form.add(fileField);
        AjaxSubmitLink link = new ChooserSubmitLink();
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);
        form.add(link);
    }

    @Override
    public ImportData createImportSource() throws IOException {
        File file = new File(this.file);
        return FileData.createFromFile(file);
    }

    protected void initFileChooser(GeoServerFileChooser fileChooser) {
        // chooser.setFilter(new Model(new ExtensionFile"(".shp")));
    }

    private class ChooserSubmitLink extends AjaxSubmitLink {
        public ChooserSubmitLink() {
            super("chooser");
        }

        @Override
        protected void onSubmit(AjaxRequestTarget target) {
            dialog.setTitle(new ParamResourceModel("chooseFile", this));
            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                @Override
                protected Component getContents(String id) {
                    // use what the user currently typed
                    File file = null;
                    if (!fileField.getInput().trim().equals("")) {
                        file = new File(fileField.getInput());
                        if (!file.exists()) file = null;
                    }

                    GeoServerFileChooser chooser = new GeoServerFileChooser(id, new Model<>(file)) {
                        @Override
                        protected void fileClicked(File file, Optional<AjaxRequestTarget> target) {
                            SpatialFilePanel.this.file = file.getAbsolutePath();

                            fileField.clearInput();
                            fileField.setModelObject(file.getAbsolutePath());
                            if (target.isPresent()) {
                                target.get().add(fileField);
                                dialog.close(target.get());
                            }
                        }
                    };

                    initFileChooser(chooser);
                    return chooser;
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    GeoServerFileChooser chooser = (GeoServerFileChooser) contents;
                    file = ((File) chooser.getDefaultModelObject()).getAbsolutePath();

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
    }
}
