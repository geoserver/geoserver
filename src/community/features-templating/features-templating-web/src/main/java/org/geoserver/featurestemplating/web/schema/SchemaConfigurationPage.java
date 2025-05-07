/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.featurestemplating.configuration.schema.SchemaFileManager;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaService;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;

// TODO WICKET8 - Verify this page works OK
public class SchemaConfigurationPage extends GeoServerSecuredPage {

    protected AjaxTabbedPanel<ITab> schemaPanel;

    private boolean isNew;

    CodeMirrorEditor editor;

    private Form<SchemaInfo> form;

    private SchemaInfoDataPanel dataPanel;

    String rawSchema;

    public SchemaConfigurationPage(IModel<SchemaInfo> model, boolean isNew) {
        this.isNew = isNew;
        initUI(model);
    }

    private void initUI(IModel<SchemaInfo> model) {
        form = new Form<>("schemaForm", model);
        List<ITab> tabs = new ArrayList<>();
        //        PanelCachingTab previewTab = new PanelCachingTab(new AbstractTab(new Model<>("Preview")) {
        //            @Override
        //            public Panel getPanel(String id) {
        //                previewPanel = new TemplatePreviewPanel(id, SchemaConfigurationPage.this);
        //                return previewPanel;
        //            }
        //        });
        PanelCachingTab dataTab = new PanelCachingTab(new AbstractTab(new Model<>("Data")) {
            @Override
            public Panel getPanel(String id) {
                return dataPanel = new SchemaInfoDataPanel(id, SchemaConfigurationPage.this);
            }
        });
        tabs.add(dataTab);
        schemaPanel = newTabbedPanel(tabs);
        schemaPanel.setMarkupId("schema-info-tabbed-panel");
        schemaPanel.setOutputMarkupId(true);
        form.add(schemaPanel);

        this.rawSchema = getStringSchema(model.getObject());
        String mode;
        if (!isNew && model.getObject().getExtension().equals("json")) mode = "javascript";
        else mode = "xml";
        editor = new CodeMirrorEditor("schemaEditor", mode, new PropertyModel<>(this, "rawSchema")) {
            @Override
            public boolean isRequired() {
                boolean result = false;
                IFormSubmitter submitter = form.getRootForm().findSubmitter();
                if (submitter != null) result = !submitter.equals(dataPanel.getUploadLink());
                return result;
            }
        };
        form.add(editor);
        if (mode.equals("javascript")) {
            editor.setModeAndSubMode(mode, model.getObject().getExtension());
        }
        editor.setMarkupId("schemaEditor");
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        form.setMultiPart(true);
        form.add(editor);
        form.add(getSubmit());
        form.add(new Link<SchemaInfoPage>("cancel") {
            @Override
            public void onClick() {
                doReturn(SchemaInfoPage.class);
            }
        });
        add(form);
    }

    private String getStringSchema(SchemaInfo schemaInfo) {
        String rawSchema = "";
        if (!isNew) {
            Resource resource = SchemaFileManager.get().getSchemaResource(schemaInfo);
            try {
                rawSchema = FileUtils.readFileToString(resource.file(), Charset.forName("UTF-8"));
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }
        return rawSchema;
    }

    public String getStringSchemaFromInput() {
        String rawSchema = getEditor().getInput();
        if (rawSchema == null || rawSchema.trim().equals(""))
            rawSchema = getEditor().getModelObject();
        return rawSchema;
    }

    public void setRawSchema(Reader in) throws IOException {
        try (BufferedReader bin = in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in)) {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = bin.readLine()) != null) {
                builder.append(line).append("\n");
            }

            this.rawSchema = builder.toString();
            editor.setModelObject(rawSchema);
        }
    }

    private AjaxSubmitLink getSubmit() {
        AjaxSubmitLink submitLink = new AjaxSubmitLink("save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);
                clearFeedbackMessages();
                SchemaInfo templateInfo = form.getModelObject();
                target.add(topFeedbackPanel);
                target.add(bottomFeedbackPanel);
                String rawTemplate = SchemaConfigurationPage.this.rawSchema;
                saveSchemaInfo(templateInfo, rawTemplate);
            }

            @Override
            protected void onAfterSubmit(AjaxRequestTarget target) {
                super.onAfterSubmit(target);
                doReturn(SchemaInfoPage.class);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                addFeedbackPanels(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(editor.getSaveDecorator());
            }
        };
        return submitLink;
    }

    public void setRawSchema(String rawTemplate) {
        this.rawSchema = rawTemplate;
    }

    public String getRawSchema() {
        return rawSchema;
    }

    Form<SchemaInfo> getForm() {
        return form;
    }

    public IModel<SchemaInfo> getSchemaInfoModel() {
        return form.getModel();
    }

    void saveSchemaInfo(SchemaInfo schemaInfo, String rawTemplate) {
        new SchemaService().saveOrUpdate(schemaInfo, rawTemplate);
    }

    public CodeMirrorEditor getEditor() {
        return this.editor;
    }

    private void clearFeedbackMessages() {
        this.topFeedbackPanel.getFeedbackMessages().clear();
        this.bottomFeedbackPanel.getFeedbackMessages().clear();
    }

    private AjaxTabbedPanel<ITab> newTabbedPanel(List<ITab> tabs) {
        return new AjaxTabbedPanel<ITab>("schemaPanel", tabs) {
            @Override
            protected String getTabContainerCssClass() {
                return "tab-row tab-row-compact";
            }

            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {

                AjaxSubmitLink link = new AjaxSubmitLink(linkId) {

                    private static final long serialVersionUID = 4599409150448651749L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        SchemaInfo schemaInfo = SchemaConfigurationPage.this.form.getModelObject();
                        //                        if (!validateAndReport(schemaInfo)) return;
                        String rawSchema = getStringSchemaFromInput();
                        saveSchemaInfo(schemaInfo, rawSchema);
                        setSelectedTab(index);
                        target.add(schemaPanel);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        addFeedbackPanels(target);
                    }
                };
                return link;
            }
        };
    }
}
