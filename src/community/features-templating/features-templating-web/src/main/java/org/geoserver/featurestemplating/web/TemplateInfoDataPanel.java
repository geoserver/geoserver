/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

public abstract class TemplateInfoDataPanel extends Panel {

    static final Logger LOGGER = Logging.getLogger(TemplateInfoDataPanel.class);

    private TemplateConfigurationPage page;

    private IModel<TemplateInfo> model;

    private TextField<String> templateName;

    private DropDownChoice<String> wsDropDown;

    private DropDownChoice<String> templateExtension;

    private DropDownChoice<String> ftiDropDown;

    private FileUploadField fileUploadField;

    private AjaxSubmitLink uploadLink;

    public TemplateInfoDataPanel(String id, TemplateConfigurationPage page) {
        super(id);
        this.page = page;
        this.model = page.getTemplateInfoModel();
        initUI();
    }

    private void initUI() {
        templateName = new TextField<>("templateName", new PropertyModel<>(model, "templateName"));
        templateName.setOutputMarkupId(true);
        templateName.setRequired(true);
        add(templateName);
        templateExtension =
                new DropDownChoice<>(
                        "extension", new PropertyModel<>(model, "extension"), getExtensions());
        CodeMirrorEditor editor = page.getEditor();
        templateExtension.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        String mode = templateExtension.getConvertedInput();
                        if (mode != null && (mode.equals("xml") || mode.equals("xhtml")))
                            editor.setMode("xml");
                        else if (isJsonLd(editor)) editor.setModeAndSubMode("javascript", "jsonld");
                        else editor.setModeAndSubMode("javascript", mode);
                        ajaxRequestTarget.add(editor);
                        TemplatePreviewPanel panel = getPreviewPanel();
                        if (panel != null)
                            panel.setOutputFormatsDropDownValues(
                                    templateExtension.getModelObject());
                    }
                });
        templateExtension.setRequired(true);
        add(templateExtension);
        wsDropDown =
                new DropDownChoice<>(
                        "workspace", new PropertyModel<>(model, "workspace"), getWorkspaces());
        wsDropDown.setNullValid(true);
        wsDropDown.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 732177308220189475L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        String workspace = wsDropDown.getConvertedInput();
                        ftiDropDown.setChoices(getFeatureTypesInfo(workspace));
                        ftiDropDown.modelChanged();
                        target.add(ftiDropDown);
                        ftiDropDown.setEnabled(true);
                        TemplatePreviewPanel previewPanel = getPreviewPanel();
                        if (previewPanel != null) previewPanel.setWorkspaceValue(workspace);
                    }
                });
        add(wsDropDown);

        ftiDropDown =
                new DropDownChoice<>(
                        "featureTypeInfo",
                        new PropertyModel<>(model, "featureType"),
                        Collections.emptyList());
        if (wsDropDown.getValue() == null || wsDropDown.getValue() == "-1")
            ftiDropDown.setEnabled(false);
        else ftiDropDown.setChoices(getFeatureTypesInfo(wsDropDown.getModelObject()));
        ftiDropDown.add(
                new OnChangeAjaxBehavior() {

                    private static final long serialVersionUID = 3510850205685746576L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        TemplatePreviewPanel previewPanel = getPreviewPanel();
                        if (previewPanel != null)
                            previewPanel.setFeatureTypeInfoValue(ftiDropDown.getConvertedInput());
                    }
                });
        ftiDropDown.setOutputMarkupId(true);
        ftiDropDown.setNullValid(true);
        add(ftiDropDown);
        fileUploadField = new FileUploadField("filename");
        // Explicitly set model so this doesn't use the form model
        fileUploadField.setDefaultModel(new Model<>(""));
        add(fileUploadField);

        uploadLink = uploadLink();

        add(uploadLink);
    }

    private List<String> getWorkspaces() {
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        return catalog.getWorkspaces().stream().map(w -> w.getName()).collect(Collectors.toList());
    }

    private List<String> getExtensions() {
        return Arrays.asList("xml", "xhtml", "json");
    }

    private List<String> getFeatureTypesInfo(String workspaceName) {
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        NamespaceInfo namespaceInfo = catalog.getNamespaceByPrefix(workspaceName);
        return catalog.getFeatureTypesByNamespace(namespaceInfo).stream()
                .map(fti -> fti.getName())
                .collect(Collectors.toList());
    }

    AjaxSubmitLink uploadLink() {
        return new ConfirmOverwriteSubmitLink("upload", page.getForm()) {

            private static final long serialVersionUID = 658341311654601761L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                FileUpload upload = fileUploadField.getFileUpload();
                if (upload == null) {
                    warn("No file selected.");
                    return;
                }
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try {
                    IOUtils.copy(upload.getInputStream(), bout);
                    page.getEditor().reset();
                    page.setRawTemplate(
                            new InputStreamReader(
                                    new ByteArrayInputStream(bout.toByteArray()), "UTF-8"));
                    upload.getContentType();
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                } catch (Exception e) {
                    page.error(
                            "Errors occurred uploading the '"
                                    + upload.getClientFileName()
                                    + "' template");
                    LOGGER.log(
                            Level.WARNING,
                            "Errors occurred uploading the '"
                                    + upload.getClientFileName()
                                    + "' template",
                            e);
                }

                TemplateInfo templateInfo = model.getObject();
                // set it
                String fileName = upload.getClientFileName();
                if (templateInfo.getTemplateName() == null
                        || "".equals(templateInfo.getTemplateName().trim())) {
                    templateName.setModelValue(
                            new String[] {ResponseUtils.stripExtension(fileName)});
                }
                int index = fileName.lastIndexOf(".");
                String extension = fileName.substring(index + 1);
                templateInfo.setExtension(extension);
                CodeMirrorEditor editor = page.getEditor();
                if (!extension.equals("xml")) {
                    if (isJsonLd(editor)) editor.setModeAndSubMode("javascript", "jsonld");
                    else editor.setModeAndSubMode("javascript", "json");
                } else {
                    editor.setMode(extension);
                }
                editor.modelChanged();
                templateName.modelChanged();
                templateExtension.modelChanged();
                target.add(editor);
                target.add(page);
            }
        };
    }

    class ConfirmOverwriteSubmitLink extends AjaxSubmitLink {

        private static final long serialVersionUID = 2673499149884774636L;

        public ConfirmOverwriteSubmitLink(String id, Form<?> form) {
            super(id, form);
        }

        @Override
        protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
            super.updateAjaxAttributes(attributes);
            attributes
                    .getAjaxCallListeners()
                    .add(
                            new AjaxCallListener() {
                                /** serialVersionUID */
                                private static final long serialVersionUID = 8637613472102572505L;

                                @Override
                                public CharSequence getPrecondition(Component component) {
                                    CharSequence message =
                                            new ParamResourceModel(
                                                            "confirmOverwrite",
                                                            TemplateInfoDataPanel.this)
                                                    .getString();
                                    message = JavaScriptUtils.escapeQuotes(message);
                                    return "var val = attrs.event.view.document.gsEditors ? "
                                            + "attrs.event.view.document.gsEditors."
                                            + page.getEditor().getTextAreaMarkupId()
                                            + ".getValue() : "
                                            + "attrs.event.view.document.getElementById(\""
                                            + page.getEditor().getTextAreaMarkupId()
                                            + "\").value; "
                                            + "if(val != '' &&"
                                            + "!confirm('"
                                            + message
                                            + "')) return false;";
                                }
                            });
        }

        @Override
        public boolean getDefaultFormProcessing() {
            return false;
        }
    }

    protected abstract TemplatePreviewPanel getPreviewPanel();

    boolean isJsonLd(CodeMirrorEditor editor) {
        String template = editor.getModelObject();
        if (template != null && !template.equals("") && template.contains("@context")) return true;
        return false;
    }

    public AjaxSubmitLink getUploadLink() {
        return uploadLink;
    }
}
