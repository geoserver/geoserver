/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;

public class BulkImportPanel extends Panel {

    private static final long serialVersionUID = -7787191736336649903L;

    public BulkImportPanel(String id) {
        super(id);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog = new GeoServerDialog("dialog");
        add(dialog);
        dialog.setInitialHeight(100);

        ArrayList<String> list = new ArrayList<String>();
        for (Configuration template : TaskManagerBeans.get().getDao().getConfigurations(true)) {
            list.add(template.getName());
        }

        DropDownChoice<String> ddTemplate =
                new DropDownChoice<String>("template", Model.of(), list);
        add(ddTemplate.setRequired(true));

        FileUploadField fileUpload = new FileUploadField("fileUpload");
        add(fileUpload.setRequired(true));

        CheckBox cbValidated = new CheckBox("validate", Model.of(true));
        add(cbValidated);

        AjaxSubmitLink importButton =
                new AjaxSubmitLink("import") {
                    private static final long serialVersionUID = -3288982013478650146L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        String csvData = new String(fileUpload.getFileUpload().getBytes());
                        if (csvData.isEmpty()) {
                            error(
                                    new ParamResourceModel("importEmpty", BulkImportPanel.this)
                                            .getString());

                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        } else {
                            dialog.showOkCancel(
                                    target,
                                    new DialogDelegate() {
                                        private static final long serialVersionUID =
                                                -8203963847815744909L;

                                        @Override
                                        protected Component getContents(String id) {
                                            return new Label(
                                                    id,
                                                    new ParamResourceModel(
                                                            "importWarning",
                                                            BulkImportPanel.this,
                                                            numberOfLines(csvData)));
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            try {
                                                if (TaskManagerBeans.get()
                                                        .getImportTool()
                                                        .doImportWithTemplate(
                                                                ddTemplate.getModelObject(),
                                                                csvData,
                                                                cbValidated.getModelObject())) {
                                                    info(
                                                            new ParamResourceModel(
                                                                            "importSuccess",
                                                                            BulkImportPanel.this)
                                                                    .getString());
                                                } else {
                                                    error(
                                                            new ParamResourceModel(
                                                                            "importFailure",
                                                                            BulkImportPanel.this)
                                                                    .getString());
                                                }
                                            } catch (IOException e) {
                                                Throwable rootCause =
                                                        ExceptionUtils.getRootCause(e);
                                                error(
                                                        rootCause == null
                                                                ? e.getLocalizedMessage()
                                                                : rootCause.getLocalizedMessage());
                                            }
                                            ((GeoServerBasePage) getPage())
                                                    .addFeedbackPanels(target);
                                            return true;
                                        }
                                    });
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                    }
                };
        add(importButton);
    }

    private static int numberOfLines(String data) {
        int count = -1;
        try (Scanner scanner = new Scanner(data)) {
            while (scanner.hasNextLine()) {
                if (!scanner.nextLine().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }
}
