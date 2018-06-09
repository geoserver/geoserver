/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.web.ConfigurationPage;
import org.geoserver.taskmanager.web.panel.FileUploadPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FileUploadAction
 *
 * @author Timothy De Bock
 */
@Component
public class FileUploadAction implements Action {

    private static final long serialVersionUID = 4996136164811697150L;

    private static final String NAME = "FileUpload";

    private static class DialogDelegate extends GeoServerDialog.DialogDelegate {

        private static final long serialVersionUID = 7410393012930249966L;

        private FileUploadPanel panel;

        private ConfigurationPage onPage;

        private IModel<String> valueModel;

        private String fileService;

        public DialogDelegate(
                ConfigurationPage onPage, IModel<String> valueModel, String fileService) {
            this.onPage = onPage;
            this.valueModel = valueModel;
            this.fileService = fileService;
        }

        @Override
        protected org.apache.wicket.Component getContents(String id) {
            panel = new FileUploadPanel(id, valueModel, fileService);
            return panel;
        }

        @Override
        protected boolean onSubmit(AjaxRequestTarget target, org.apache.wicket.Component contents) {
            panel.onSubmit();
            onPage.addAttributesPanel(target);
            return true;
        }

        @Override
        public void onError(AjaxRequestTarget target, Form<?> form) {
            target.add(panel.getFeedbackPanel());
        }
    }

    @Autowired private LookupService<FileService> fileServices;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(
            ConfigurationPage onPage,
            AjaxRequestTarget target,
            IModel<String> valueModel,
            List<String> dependentValues) {
        FileService fileService;
        if (dependentValues.size() > 0) {
            fileService = fileServices.get(dependentValues.get(0));
        } else {
            fileService = null;
        }
        GeoServerDialog dialog = onPage.getDialog();

        dialog.setTitle(new ParamResourceModel("FileUploadPanel.dialogTitle", onPage.getPage()));
        dialog.setInitialWidth(650);
        dialog.setInitialHeight(300);
        dialog.showOkCancel(target, new DialogDelegate(onPage, valueModel, fileService.getName()));
    }

    @Override
    public boolean accept(String value, List<String> dependentValues) {
        FileService fileService;
        if (dependentValues.size() > 0 && dependentValues.get(0) != null) {
            fileService = fileServices.get(dependentValues.get(0));
        } else {
            fileService = null;
        }
        return fileService != null;
    }
}
