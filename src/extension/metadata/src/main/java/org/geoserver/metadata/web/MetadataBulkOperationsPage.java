/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.geoserver.metadata.data.model.impl.GlobalModel;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.data.service.GlobalModelService;
import org.geoserver.metadata.data.service.MetaDataBulkService;
import org.geoserver.metadata.web.panel.ProgressPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** @author Niels Charlier */
public class MetadataBulkOperationsPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 2273966783474224452L;

    private ProgressPanel progressPanel;

    private GlobalModel<String> errorModel = new GlobalModel<>();

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog;
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(100);
        ((ModalWindow) dialog.get("dialog")).showUnloadConfirmation(false);

        add(progressPanel = new ProgressPanel("progress"));

        add(fixButton(dialog));

        Form<?> formImport = new Form<>("formImport");
        add(formImport);

        DropDownChoice<String> geonetworkName = createGeonetworkDropDown();
        formImport.add(geonetworkName.setRequired(true));

        FileUploadField csvImport = new FileUploadField("importCsv");
        formImport.add(csvImport.setRequired(true));

        formImport.add(importButton(dialog, geonetworkName, csvImport));

        Form<?> formCustom = new Form<>("formCustom");
        add(formCustom);

        TextField<String> ruleList = new TextField<>("ruleList", new Model<String>());
        formCustom.add(ruleList);

        FileUploadField csvCustom = new FileUploadField("customCsv");
        formCustom.add(csvCustom);

        formCustom.add(nativeToCustomButton(dialog, ruleList, csvCustom));

        Form<?> formClear = new Form<>("formClear");
        add(formClear);

        CheckBox clearTemplates = new CheckBox("clearTemplates", new Model<Boolean>());

        formClear.add(clearTemplates);

        formClear.add(clearButton(dialog, clearTemplates));
    }

    private AjaxSubmitLink clearButton(GeoServerDialog dialog, CheckBox clearTemplates) {
        return new AjaxSubmitLink("clear") {
            private static final long serialVersionUID = 6765654318639597167L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {

                dialog.showOkCancel(target, dialogDelegate());
            }

            private DialogDelegate dialogDelegate() {
                return new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = -9025890060280394005L;

                    private boolean ok = false;

                    @Override
                    protected Component getContents(String id) {
                        return new Label(
                                id,
                                new ParamResourceModel(
                                        "clearWarning",
                                        MetadataBulkOperationsPage.this,
                                        numberOfLayers()));
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        ok = true;
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        if (ok) {
                            GlobalModel<Float> progressModel = new GlobalModel<>(0.0f);
                            run(progressModel);
                            startProgress(target, progressModel, "clearing");
                        }
                    }

                    private void run(GlobalModel<Float> progressModel) {
                        MetaDataBulkService service =
                                GeoServerApplication.get()
                                        .getApplicationContext()
                                        .getBean(MetaDataBulkService.class);
                        GlobalModelService globalModelService =
                                GeoServerApplication.get()
                                        .getApplicationContext()
                                        .getBean(GlobalModelService.class);

                        Authentication auth = getSecContext().getAuthentication();

                        Runnable clearRunnable =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        getSecContext().setAuthentication(auth);
                                        try {
                                            service.clearAll(
                                                    clearTemplates.getModelObject(),
                                                    progressModel.getKey());
                                        } catch (IOException e) {
                                            globalModelService.put(
                                                    errorModel.getKey(), e.getLocalizedMessage());
                                        }
                                    }
                                };
                        Executors.newSingleThreadExecutor().execute(clearRunnable);
                    }
                };
            }
        };
    }

    private AjaxSubmitLink nativeToCustomButton(
            GeoServerDialog dialog, TextField<String> ruleList, FileUploadField csvCustom) {
        return new AjaxSubmitLink("custom") {
            private static final long serialVersionUID = 6765654318639597167L;

            @Override
            public void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {

                String csvData =
                        csvCustom.getFileUpload() == null
                                ? null
                                : new String(csvCustom.getFileUpload().getBytes());

                List<Integer> indexes;
                try {
                    indexes = convertToList(ruleList.getModelObject());
                } catch (NumberFormatException e) {
                    error(
                            new StringResourceModel(
                                            "customRuleFormat", MetadataBulkOperationsPage.this)
                                    .getString());
                    addFeedbackPanels(target);
                    return;
                }

                dialog.showOkCancel(target, dialogDelegate(csvData, indexes));
            }

            private DialogDelegate dialogDelegate(String csvData, List<Integer> indexes) {
                return new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = -9025890060280394005L;

                    private boolean ok = false;

                    @Override
                    protected Component getContents(String id) {
                        return new Label(
                                id,
                                new ParamResourceModel(
                                        "customWarning",
                                        MetadataBulkOperationsPage.this,
                                        csvData == null
                                                ? numberOfLayers()
                                                : numberOfLines(csvData)));
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        ok = true;
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        if (ok) {
                            GlobalModel<Float> progressModel = new GlobalModel<>(0.0f);
                            run(csvData, indexes, progressModel);
                            startProgress(target, progressModel, "customing");
                        }
                    }
                };
            }

            private void run(
                    String csvData, List<Integer> indexes, GlobalModel<Float> progressModel) {
                MetaDataBulkService service =
                        GeoServerApplication.get()
                                .getApplicationContext()
                                .getBean(MetaDataBulkService.class);
                GlobalModelService globalModelService =
                        GeoServerApplication.get()
                                .getApplicationContext()
                                .getBean(GlobalModelService.class);
                String errorNoSuccess =
                        new StringResourceModel("customNoSuccess", MetadataBulkOperationsPage.this)
                                .getString();

                Authentication auth = getSecContext().getAuthentication();

                Runnable nativeToCustomRunnable =
                        new Runnable() {
                            @Override
                            public void run() {
                                getSecContext().setAuthentication(auth);
                                if (csvData == null) {
                                    service.nativeToCustom(indexes, progressModel.getKey());
                                } else {
                                    boolean success =
                                            service.nativeToCustom(
                                                    indexes, csvData, progressModel.getKey());
                                    if (!success) {
                                        globalModelService.put(errorModel.getKey(), errorNoSuccess);
                                    }
                                }
                            }
                        };
                Executors.newSingleThreadExecutor().execute(nativeToCustomRunnable);
            }
        };
    }

    private AjaxSubmitLink importButton(
            GeoServerDialog dialog,
            DropDownChoice<String> geonetworkName,
            FileUploadField csvImport) {
        return new AjaxSubmitLink("import") {
            private static final long serialVersionUID = 6765654318639597167L;

            @Override
            public void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {

                String csvData = new String(csvImport.getFileUpload().getBytes());

                dialog.showOkCancel(target, dialogDelegate(geonetworkName, csvData));
            }

            private DialogDelegate dialogDelegate(
                    DropDownChoice<String> geonetworkName, String csvData) {
                return new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = -9025890060280394005L;

                    private boolean ok = false;

                    @Override
                    protected Component getContents(String id) {
                        return new Label(
                                id,
                                new ParamResourceModel(
                                        "importWarning",
                                        MetadataBulkOperationsPage.this,
                                        numberOfLines(csvData)));
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        ok = true;
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        if (ok) {
                            GlobalModel<Float> progressModel = new GlobalModel<>(0.0f);
                            run(progressModel);
                            startProgress(target, progressModel, "importing");
                        }
                    }

                    private void run(GlobalModel<Float> progressModel) {
                        MetaDataBulkService service =
                                GeoServerApplication.get()
                                        .getApplicationContext()
                                        .getBean(MetaDataBulkService.class);
                        GlobalModelService globalModelService =
                                GeoServerApplication.get()
                                        .getApplicationContext()
                                        .getBean(GlobalModelService.class);
                        String errorNoSuccess =
                                new StringResourceModel(
                                                "importNoSuccess", MetadataBulkOperationsPage.this)
                                        .getString();

                        Authentication auth = getSecContext().getAuthentication();

                        Runnable importRunnable =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        getSecContext().setAuthentication(auth);
                                        String geonetworkNameStr =
                                                geonetworkName.getDefaultModelObject().toString();
                                        boolean success =
                                                service.importAndLink(
                                                        geonetworkNameStr,
                                                        csvData,
                                                        progressModel.getKey());
                                        if (!success) {
                                            globalModelService.put(
                                                    errorModel.getKey(), errorNoSuccess);
                                        }
                                    }
                                };
                        Executors.newSingleThreadExecutor().execute(importRunnable);
                    }
                };
            }
        };
    }

    private AjaxLink<Object> fixButton(GeoServerDialog dialog) {
        return new AjaxLink<Object>("fix") {
            private static final long serialVersionUID = 4636152085574084063L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {
                            private static final long serialVersionUID = 1462655770445974740L;

                            private boolean ok = false;

                            @Override
                            protected Component getContents(String id) {
                                return new Label(
                                        id,
                                        new ParamResourceModel(
                                                "fixWarning",
                                                MetadataBulkOperationsPage.this,
                                                numberOfLayers()));
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                ok = true;
                                return true;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target) {
                                if (ok) {
                                    GlobalModel<Float> progressModel = new GlobalModel<Float>(0.0f);
                                    MetaDataBulkService service =
                                            GeoServerApplication.get()
                                                    .getApplicationContext()
                                                    .getBean(MetaDataBulkService.class);

                                    Authentication auth = getSecContext().getAuthentication();

                                    Runnable fixRunnable =
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    getSecContext().setAuthentication(auth);
                                                    service.fixAll(progressModel.getKey());
                                                }
                                            };
                                    Executors.newSingleThreadExecutor().execute(fixRunnable);

                                    startProgress(target, progressModel, "fixing");
                                }
                            }
                        });
            }
        };
    }

    private DropDownChoice<String> createGeonetworkDropDown() {
        ConfigurationService configService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        MetadataConfiguration configuration = configService.getMetadataConfiguration();
        ArrayList<String> optionsGeonetwork = new ArrayList<>();
        if (configuration != null && configuration.getGeonetworks() != null) {
            for (GeonetworkConfiguration geonetwork : configuration.getGeonetworks()) {
                optionsGeonetwork.add(geonetwork.getName());
            }
        }
        return new DropDownChoice<>("geonetworkName", new Model<String>(), optionsGeonetwork);
    }

    private int numberOfLayers() {
        Catalog catalog = GeoServerApplication.get().getGeoServer().getCatalog();
        return catalog.getResources(ResourceInfo.class).size();
    }

    private static int numberOfLines(String data) {
        int count = 0;
        try (Scanner scanner = new Scanner(data)) {
            while (scanner.hasNextLine()) {
                if (!scanner.nextLine().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<Integer> convertToList(String indexes) {
        if (indexes != null) {
            return Arrays.stream(indexes.split(","))
                    .map(s -> Integer.parseInt(s.trim()))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private void startProgress(
            AjaxRequestTarget target, GlobalModel<Float> progressModel, String title) {
        progressPanel.setTitle(new StringResourceModel(title, this));
        progressPanel.start(
                target,
                progressModel,
                new ProgressPanel.EventHandler() {
                    private static final long serialVersionUID = 8967087707332457974L;

                    @Override
                    public void onFinished(AjaxRequestTarget target) {
                        progressModel.cleanUp();
                        if (errorModel.getObject() != null) {
                            error(errorModel.getObject());
                            errorModel.cleanUp();
                            addFeedbackPanels(target);
                        }
                    }

                    @Override
                    public void onCanceled(AjaxRequestTarget target) {
                        progressModel.cleanUp();
                    }
                });
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.ADMIN;
    }

    private static SecurityContext getSecContext() {
        return SecurityContextHolder.getContext();
    }
}
