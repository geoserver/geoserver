/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDao;
import org.geoserver.platform.exception.GeoServerException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;

public class TemplateConfigurationPage extends GeoServerSecuredPage {

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    private boolean isNew;

    CodeMirrorEditor editor;

    private Form<TemplateInfo> form;

    private TemplatePreviewPanel previewPanel;

    private transient TemplateCareTaker templateCareTaker;

    String rawTemplate;

    public TemplateConfigurationPage(IModel<TemplateInfo> model, boolean isNew) {
        this.isNew = isNew;
        this.templateCareTaker = new TemplateCareTaker();
        initUI(model);
    }

    private void initUI(IModel<TemplateInfo> model) {
        form = new Form<>("theForm", model);
        List<ITab> tabs = new ArrayList<>();
        PanelCachingTab previewTab =
                new PanelCachingTab(
                        new AbstractTab(new Model<>("Preview")) {
                            @Override
                            public Panel getPanel(String id) {
                                previewPanel =
                                        new TemplatePreviewPanel(
                                                id, TemplateConfigurationPage.this);
                                return previewPanel;
                            }
                        });
        PanelCachingTab dataTab =
                new PanelCachingTab(
                        new AbstractTab(new Model<>("Data")) {
                            @Override
                            public Panel getPanel(String id) {
                                return new TemplateInfoDataPanel(
                                        id, TemplateConfigurationPage.this) {
                                    @Override
                                    protected TemplatePreviewPanel getPreviewPanel() {
                                        return previewPanel;
                                    }
                                };
                            }
                        });
        tabs.add(dataTab);
        tabs.add(previewTab);
        tabbedPanel =
                new AjaxTabbedPanel<ITab>("tabbedPanel", tabs) {
                    @Override
                    protected String getTabContainerCssClass() {
                        return "tab-row tab-row-compact";
                    }

                    @Override
                    protected WebMarkupContainer newLink(String linkId, final int index) {

                        AjaxSubmitLink link =
                                new AjaxSubmitLink(linkId) {

                                    private static final long serialVersionUID =
                                            4599409150448651749L;

                                    @Override
                                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        TemplateInfo templateInfo =
                                                TemplateConfigurationPage.this.form
                                                        .getModelObject();
                                        String rawTemplate = getStringTemplateFromInput();
                                        if (!validateAndReport(templateInfo, rawTemplate)) return;
                                        saveTemplateInfo(templateInfo, rawTemplate);
                                        setSelectedTab(index);
                                        target.add(tabbedPanel);
                                    }

                                    @Override
                                    protected void onAfterSubmit(
                                            AjaxRequestTarget target, Form<?> form) {
                                        if (form.hasError()) addFeedbackPanels(target);
                                    }
                                };
                        return link;
                    }
                };
        tabbedPanel.setMarkupId("template-info-tabbed-panel");
        tabbedPanel.setOutputMarkupId(true);
        form.add(tabbedPanel);

        this.rawTemplate = getStringTemplate(model.getObject());
        String mode;
        if (!isNew && model.getObject().getExtension().equals("json")) mode = "javascript";
        else mode = "xml";
        form.add(
                editor =
                        new CodeMirrorEditor(
                                "templateEditor", mode, new PropertyModel<>(this, "rawTemplate")));
        if (mode.equals("javascript")) {
            editor.setModeAndSubMode(mode, model.getObject().getExtension());
        }
        editor.setMarkupId("templateEditor");
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        form.setMultiPart(true);
        form.add(editor);
        form.add(getSubmit());
        form.add(
                new Link<TemplateInfoPage>("cancel") {
                    @Override
                    public void onClick() {
                        TemplateInfo info = form.getModelObject();
                        if (templateCareTaker != null) templateCareTaker.undo(info, isNew);
                        doReturn(TemplateInfoPage.class);
                    }
                });
        add(form);
        if (!isNew && templateCareTaker != null)
            templateCareTaker.addMemento(model.getObject(), getEditor().getModelObject());
    }

    private String getStringTemplate(TemplateInfo templateInfo) {
        String rawTemplate = "";
        if (!isNew) {
            Resource resource = TemplateFileManager.get().getTemplateResource(templateInfo);
            try {
                rawTemplate = FileUtils.readFileToString(resource.file(), Charset.forName("UTF-8"));
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }
        return rawTemplate;
    }

    public String getStringTemplateFromInput() {
        String rawTemplate = getEditor().getInput();
        if (rawTemplate == null || rawTemplate.trim().equals(""))
            rawTemplate = getEditor().getModelObject();
        return rawTemplate;
    }

    public void setRawTemplate(Reader in) throws IOException {
        try (BufferedReader bin =
                in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in)) {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = bin.readLine()) != null) {
                builder.append(line).append("\n");
            }

            this.rawTemplate = builder.toString();
            editor.setModelObject(rawTemplate);
        }
    }

    private AjaxSubmitLink getSubmit() {
        AjaxSubmitLink submitLink =
                new AjaxSubmitLink("save", form) {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onSubmit(target, form);
                        clearFeedbackMessages();
                        target.add(topFeedbackPanel);
                        target.add(bottomFeedbackPanel);
                        TemplateInfo templateInfo = (TemplateInfo) form.getModelObject();

                        String rawTemplate = TemplateConfigurationPage.this.rawTemplate;
                        if (!validateAndReport(templateInfo, rawTemplate)) return;
                        saveTemplateInfo(templateInfo, rawTemplate);
                    }

                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (form.hasError()) {
                            addFeedbackPanels(target);
                        } else {
                            doReturn(TemplateInfoPage.class);
                        }
                    }
                };
        return submitLink;
    }

    public void setRawTemplate(String rawTemplate) {
        this.rawTemplate = rawTemplate;
    }

    public String getRawTemplate() {
        return rawTemplate;
    }

    Form<TemplateInfo> getForm() {
        return form;
    }

    public IModel<TemplateInfo> getTemplateInfoModel() {
        return form.getModel();
    }

    private boolean validateAndReport(TemplateInfo info, String rawTemplate) {
        try {
            TemplateModelsValidator validator = new TemplateModelsValidator();
            validator.validate(info, rawTemplate);
        } catch (GeoServerException e) {
            form.error(e.getMessage());
            return false;
        }
        return true;
    }

    void saveTemplateInfo(TemplateInfo templateInfo, String rawTemplate) {
        if (!validateAndReport(templateInfo, rawTemplate)) return;
        TemplateFileManager.get().saveTemplateFile(templateInfo, rawTemplate);
        TemplateInfoDao.get().saveOrUpdate(templateInfo);
        templateCareTaker.deleteOldTemplateFile(templateInfo);
    }

    public CodeMirrorEditor getEditor() {
        return this.editor;
    }

    private void clearFeedbackMessages() {
        this.topFeedbackPanel.getFeedbackMessages().clear();
        this.bottomFeedbackPanel.getFeedbackMessages().clear();
    }
}
