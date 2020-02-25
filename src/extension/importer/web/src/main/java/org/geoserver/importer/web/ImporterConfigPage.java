/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ImporterInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.DirectoryInput;

/**
 * Edits the {@link org.geoserver.importer.Importer} configuration, {@link
 * org.geoserver.importer.ImporterInfo}.
 */
public class ImporterConfigPage extends GeoServerSecuredPage {

    public ImporterConfigPage() {
        Importer importer = getGeoServerApplication().getBeanOfType(Importer.class);
        ImporterInfo info = importer.getConfiguration();
        Model<ImporterInfo> model = new Model<ImporterInfo>(info);

        Form form = new Form<>("form", model);
        add(form);

        DirectoryInput chooser =
                new DirectoryInput(
                        "uploadRoot",
                        new PropertyModel<>(model, "uploadRoot"),
                        new ParamResourceModel("directory", this),
                        false) {
                    @Override
                    protected IModel<String> getFileModel(IModel<String> paramValue) {
                        // avoid the "magic" transformations of paths, as the input might not
                        // be a path at all
                        return paramValue;
                    }
                };
        form.add(chooser);

        RangeValidator threadValidator = new RangeValidator(1, Integer.MAX_VALUE);
        TextField maxSync =
                new TextField(
                        "maxSync",
                        new PropertyModel<Integer>(model, "maxSynchronousImports"),
                        Integer.class);
        maxSync.add(threadValidator);
        form.add(maxSync);

        TextField maxAsync =
                new TextField(
                        "maxAsync",
                        new PropertyModel<Integer>(model, "maxAsynchronousImports"),
                        Integer.class);
        maxAsync.add(threadValidator);
        form.add(maxAsync);

        SubmitLink submit =
                new SubmitLink("submit", form) {
                    @Override
                    public void onSubmit() {
                        try {
                            Importer singleton =
                                    getGeoServerApplication().getBeanOfType(Importer.class);
                            singleton.setConfiguration(info);
                            doReturn();
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                };
        form.add(submit);

        Button cancel =
                new Button("cancel") {
                    public void onSubmit() {
                        doReturn();
                    }
                };
        form.add(cancel);
        cancel.setDefaultFormProcessing(false);
    }
}
