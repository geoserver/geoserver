/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.SLDNamedLayerValidator;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GSModalWindow;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.style.StyledLayerDescriptor;
import org.xml.sax.SAXParseException;

/**
 * Base page for creating/editing styles
 *
 * <p>WARNING: one crucial aspect of this page is its ability to not loose edits when one switches from one tab to the
 * other. I did not find any effective way to unit test this, so _please_, if you do modify anything in this class
 * (especially the models), manually retest that the edits are not lost on tab switch.
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public abstract class AbstractStylePage extends GeoServerSecuredPage {

    static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3}){1,2}$");

    static class ChooseColorPanel extends Panel {

        final TextField<String> chooser;
        final String initialColor;

        public ChooseColorPanel(String id, String initialColor) {
            super(id);
            chooser = new TextField<>("chooser", new Model<>());
            chooser.setOutputMarkupId(true);
            this.initialColor = initialColor;
            add(chooser);
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.render(CssReferenceHeaderItem.forReference(
                    new PackageResourceReference(AbstractStylePage.class, "js/spectrum/spectrum.css")));
            response.render(JavaScriptHeaderItem.forReference(
                    new PackageResourceReference(AbstractStylePage.class, "js/spectrum/spectrum.js")));
            response.render(
                    CssHeaderItem.forReference(new CssResourceReference(AbstractStylePage.class, "StylePage.css")));
            String enableSpectrum = "$(\"#chooser\").spectrum({\n"
                    + "    color: \""
                    + StringEscapeUtils.escapeEcmaScript(initialColor)
                    + "\",\n"
                    + "    showInput: true,\n"
                    + "    flat: true,\n"
                    + "    className: \"full-spectrum\",\n"
                    + "    showInitial: true,\n"
                    + "    showPalette: true,\n"
                    + "    showSelectionPalette: true,\n"
                    + "    showButtons: false,\n"
                    + "    preferredFormat: \"hex\",\n"
                    + "    clickoutFiresChange: true,\n"
                    + "    palette: [\n"
                    + "        [\"#000\",\"#444\",\"#666\",\"#999\",\"#ccc\",\"#eee\",\"#f3f3f3\",\"#fff\"],\n"
                    + "        [\"#f00\",\"#f90\",\"#ff0\",\"#0f0\",\"#0ff\",\"#00f\",\"#90f\",\"#f0f\"],\n"
                    + "        [\"#f4cccc\",\"#fce5cd\",\"#fff2cc\",\"#d9ead3\",\"#d0e0e3\",\"#cfe2f3\",\"#d9d2e9\",\"#ead1dc\"],\n"
                    + "        [\"#ea9999\",\"#f9cb9c\",\"#ffe599\",\"#b6d7a8\",\"#a2c4c9\",\"#9fc5e8\",\"#b4a7d6\",\"#d5a6bd\"],\n"
                    + "        [\"#e06666\",\"#f6b26b\",\"#ffd966\",\"#93c47d\",\"#76a5af\",\"#6fa8dc\",\"#8e7cc3\",\"#c27ba0\"],\n"
                    + "        [\"#c00\",\"#e69138\",\"#f1c232\",\"#6aa84f\",\"#45818e\",\"#3d85c6\",\"#674ea7\",\"#a64d79\"],\n"
                    + "        [\"#900\",\"#b45f06\",\"#bf9000\",\"#38761d\",\"#134f5c\",\"#0b5394\",\"#351c75\",\"#741b47\"],\n"
                    + "        [\"#600\",\"#783f04\",\"#7f6000\",\"#274e13\",\"#0c343d\",\"#073763\",\"#20124d\",\"#4c1130\"]],\n"
                    + "    move: function (color) {\n"
                    + "          $(\"#chooser\").spectrum(\"set\", color);\n"
                    + "    }\n"
                    + "});";
            response.render(new OnDomReadyHeaderItem(enableSpectrum));
        }
    }

    protected Form<StyleInfo> styleForm;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    protected CodeMirrorEditor editor;

    protected GSModalWindow popup;

    protected GeoServerDialog dialog;

    protected CompoundPropertyModel<StyleInfo> styleModel;
    protected IModel<LayerInfo> layerModel;

    String rawStyle;

    public AbstractStylePage() {}

    public AbstractStylePage(StyleInfo style) {
        recoverCssStyle(style);
        initPreviewLayer(style);
        initUI(style);
    }

    protected void initPreviewLayer(StyleInfo style) {
        Catalog catalog = getCatalog();
        List<LayerInfo> layers;

        // Try getting the first layer associated with this style
        if (style != null) {
            layers = catalog.getLayers(style);
            if (!layers.isEmpty()) {
                layerModel = new Model<>(layers.get(0));
                return;
            }
        }

        // Try getting the first layer in the default store in the default workspace
        WorkspaceInfo defaultWs = catalog.getDefaultWorkspace();
        if (defaultWs != null) {
            DataStoreInfo defaultStore = catalog.getDefaultDataStore(defaultWs);
            if (defaultStore != null) {
                List<ResourceInfo> resources = catalog.getResourcesByStore(defaultStore, ResourceInfo.class);
                for (ResourceInfo resource : resources) {
                    layers = catalog.getLayers(resource);
                    if (!layers.isEmpty()) {
                        layerModel = new Model<>(layers.get(0));
                        return;
                    }
                }
            }
        }

        // Try getting the first layer returned by the catalog
        layers = catalog.getLayers();
        if (!layers.isEmpty()) {
            layerModel = new Model<>(layers.get(0));
            return;
        }

        // If none of these succeeded, return an empty model
        layerModel = new Model<>(new LayerInfoImpl());
    }

    protected void initUI(StyleInfo style) {
        /* init model */
        if (style == null) {
            styleModel = new CompoundPropertyModel<>(getCatalog().getFactory().createStyle());
            styleModel.getObject().setName("");
            styleModel.getObject().setLegend(getCatalog().getFactory().createLegend());
        } else {
            if (style.getLegend() == null) {
                style.setLegend(getCatalog().getFactory().createLegend());
            }
            styleModel = new CompoundPropertyModel<>(style);
        }

        /* init main form */
        styleForm = new Form<>("styleForm", styleModel) {
            @Override
            protected void onSubmit() {
                onStyleFormSubmit();
                super.onSubmit();
            }
        };
        add(styleForm);
        styleForm.setMultiPart(true);

        /* init popup */
        popup = new GSModalWindow("popup");
        styleForm.add(popup);
        /* init tabs */
        List<ITab> tabs = new ArrayList<>();

        // Well known tabs
        PanelCachingTab dataTab = new PanelCachingTab(new AbstractTab(new Model<>("Data")) {

            @Override
            public Panel getPanel(String id) {
                return new StyleAdminPanel(id, AbstractStylePage.this);
            }
        });

        PanelCachingTab publishingTab = new PanelCachingTab(new AbstractTab(new Model<>("Publishing")) {
            @Serial
            private static final long serialVersionUID = 4184410057835108176L;

            @Override
            public Panel getPanel(String id) {
                return new LayerAssociationPanel(id, AbstractStylePage.this);
            }
        });

        PanelCachingTab previewTab = new PanelCachingTab(new AbstractTab(new Model<>("Layer Preview")) {

            @Override
            public Panel getPanel(String id) {
                return new OpenLayersPreviewPanel(id, AbstractStylePage.this);
            }
        });

        PanelCachingTab attributeTab = new PanelCachingTab(new AbstractTab(new Model<>("Layer Attributes")) {
            @Serial
            private static final long serialVersionUID = 4184410057835108176L;

            @Override
            public Panel getPanel(String id) {
                try {
                    return new LayerAttributePanel(id, AbstractStylePage.this);
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }
            }
        });
        // If style is null, this is a new style.
        // If so, we want to disable certain tabs
        tabs.add(dataTab);
        if (style != null) {
            tabs.add(publishingTab);
            tabs.add(previewTab);
            tabs.add(attributeTab);
        }

        // Dynamic tabs
        List<StyleEditTabPanelInfo> tabPanels = getGeoServerApplication().getBeansOfType(StyleEditTabPanelInfo.class);

        // sort the tabs based on order
        Collections.sort(tabPanels, (o1, o2) -> {
            Integer order1 = o1.getOrder() >= 0 ? o1.getOrder() : Integer.MAX_VALUE;
            Integer order2 = o2.getOrder() >= 0 ? o2.getOrder() : Integer.MAX_VALUE;

            return order1.compareTo(order2);
        });
        // instantiate tab panels and add to tabs list
        for (StyleEditTabPanelInfo tabPanelInfo : tabPanels) {
            String titleKey = tabPanelInfo.getTitleKey();
            IModel<String> titleModel = null;
            if (tabPanelInfo.isEnabledOnNew() || style != null) {
                if (titleKey != null) {
                    titleModel = new org.apache.wicket.model.ResourceModel(titleKey);
                } else {
                    titleModel = new Model<>(tabPanelInfo.getComponentClass().getSimpleName());
                }

                final Class<StyleEditTabPanel> panelClass = tabPanelInfo.getComponentClass();

                tabs.add(new AbstractTab(titleModel) {
                    @Serial
                    private static final long serialVersionUID = -6637277497986497791L;

                    @Override
                    public Panel getPanel(String panelId) {
                        StyleEditTabPanel tabPanel;
                        try {
                            tabPanel = panelClass
                                    .getConstructor(String.class, AbstractStylePage.class)
                                    .newInstance(panelId, AbstractStylePage.this);
                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                        return tabPanel;
                    }
                });
            }
        }

        tabbedPanel = new StyleTabbedPanel(tabs);
        tabbedPanel.setMarkupId("style-context");
        tabbedPanel.setOutputMarkupId(true);

        styleForm.add(tabbedPanel);

        /* init editor */
        styleForm.add(
                editor = new CodeMirrorEditor(
                        "styleEditor", styleHandler().getCodeMirrorEditMode(), new PropertyModel<>(this, "rawStyle")));
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setTextAreaMarkupId("editor");
        editor.setMarkupId("style-editor");
        editor.setOutputMarkupId(true);
        editor.setRequired(true);
        styleForm.add(editor);

        // insert picture button
        dialog = new GeoServerDialog("dialog");
        dialog.setOutputMarkupId(true);
        add(dialog);
        editor.addCustomButton(
                new ParamResourceModel("insertImage", getPage()).getString(),
                "button-picture",
                new InsertImageButton());

        editor.addCustomButton(
                new ParamResourceModel("chooseColor", getPage()).getString(), "button-color", new ChooseColorButton());

        add(validateLink());
        add(new AjaxSubmitLink("apply", styleForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                // If we have a new style, go to the edit page
                if (style == null) {
                    StyleInfo s = getStyleInfo();
                    PageParameters parameters = new PageParameters();
                    parameters.add(StyleEditPage.NAME, s.getName());
                    if (s.getWorkspace() != null) {
                        parameters.add(StyleEditPage.WORKSPACE, s.getWorkspace().getName());
                    }
                    getRequestCycle().setResponsePage(StyleEditPage.class, parameters);
                }
                addFeedbackPanels(target);
                // Update preview if we are on the preview tab
                if (style != null && tabbedPanel.getSelectedTab() == 2) {
                    tabbedPanel.visitChildren(StyleEditTabPanel.class, (component, visit) -> {
                        if (component instanceof OpenLayersPreviewPanel previewPanel) {
                            try {
                                target.appendJavaScript(previewPanel.getUpdateCommand());
                            } catch (Exception e) {
                                LOGGER.log(Level.FINER, e.getMessage(), e);
                            }
                        }
                    });
                }
            }

            @Override
            protected void onAfterSubmit(AjaxRequestTarget target) {
                // Re-initialize the Legend model object, if it is null.
                if (styleModel.getObject().getLegend() == null) {
                    styleModel.getObject().setLegend(getCatalog().getFactory().createLegend());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                addFeedbackPanels(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(editor.getSaveDecorator());
            }
        });
        add(new AjaxSubmitLink("save", styleForm) {
            @Override
            protected void onAfterSubmit(AjaxRequestTarget target) {
                if (getForm().hasError()) {
                    addFeedbackPanels(target);
                } else {
                    doReturn(StylePage.class);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                addFeedbackPanels(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(editor.getSaveDecorator());
            }
        });
        Link<StylePage> cancelLink = new Link<>("cancel") {
            @Override
            public void onClick() {
                doReturn(StylePage.class);
            }
        };
        add(cancelLink);

        // add additional style components that may be used e.g. by extensions
        List<StyleComponentInfo> compInfo = getGeoServerApplication().getBeansOfType(StyleComponentInfo.class);
        for (StyleComponentInfo comp : compInfo) {
            try {
                Class<?> component = comp.getComponentClass();
                Component c = (Component) component
                        .getConstructor(String.class, AbstractStylePage.class)
                        .newInstance(comp.getId(), this);
                styleForm.add(c);
            } catch (Exception e) {
                throw new WicketRuntimeException(e);
            }
        }
    }

    StyleHandler styleHandler() {
        String format = styleModel.getObject().getFormat();
        if (format == null) {
            return Styles.handler(SLDHandler.FORMAT);
        }
        return Styles.handler(format);
    }

    Component validateLink() {
        return new GeoServerAjaxFormLink("validate", styleForm) {

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                editor.processInput();

                List<Exception> errors = validateSLD();

                if (errors.isEmpty()) {
                    form.info("No validation errors.");
                } else {
                    for (Exception e : errors) {
                        form.error(sldErrorWithLineNo(e));
                    }
                }
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(editor.getSaveDecorator());
            }
        };
    }

    private String sldErrorWithLineNo(Exception e) {
        if (e instanceof SAXParseException se) {
            return "line " + se.getLineNumber() + ": " + e.getLocalizedMessage();
        }
        String message = e.getLocalizedMessage();
        if (message != null) {
            return message;
        } else {
            return new ParamResourceModel("genericError", this).getString();
        }
    }

    List<Exception> validateSLD() {
        try {
            final String style = editor.getInput();
            List<Exception> validationErrors = styleHandler()
                    .validate(
                            new ByteArrayInputStream(style.getBytes()),
                            null,
                            getCatalog().getResourcePool().getEntityResolver());
            StyledLayerDescriptor sld = styleHandler()
                    .parse(
                            new ByteArrayInputStream(style.getBytes()),
                            null,
                            null,
                            getCatalog().getResourcePool().getEntityResolver());
            // If there are more than one layers, assume this is a style group and validate
            // accordingly.
            if (sld.getStyledLayers().length > 1) {
                validationErrors.addAll(SLDNamedLayerValidator.validate(getCatalog(), sld));
            }
            return validationErrors;
        } catch (Exception e) {
            return Arrays.asList(e);
        }
    }

    Reader readFile(StyleInfo style) throws IOException {
        ResourcePool pool = getCatalog().getResourcePool();
        return pool.readStyle(style);
    }

    public void setRawStyle(Reader in) throws IOException {
        try (BufferedReader bin = in instanceof BufferedReader br ? br : new BufferedReader(in)) {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = bin.readLine()) != null) {
                builder.append(line).append("\n");
            }

            this.rawStyle = builder.toString();
            editor.setModelObject(rawStyle);
        }
    }

    /**
     * Check for an original CSS version of the style created by the old CSS extension (pre-pluggable styles). If a CSS
     * style is found, recover it if the derived SLD has not subsequently been manually edited.
     *
     * <p>The recovery is accomplished by updating the catalog to point to the original CSS file, and changing the
     * style's format to "css".
     *
     * @param si The {@link StyleInfo} for which to check for and potentially recover a CSS version.
     */
    protected void recoverCssStyle(StyleInfo si) {
        if (si == null) {
            return;
        }

        // Only try to repair missing CSS files if a CSS style handler is registered.
        try {
            Styles.handler("css");
        } catch (Exception e) {
            LOGGER.finest(e.getLocalizedMessage()); // fool spotbugs?
            return;
        }

        // Use this tolerance to prevent erasing an SLD that was manually edited after being
        // generated from a CSS file. (Generated SLDs will always be newer than the CSS).
        long favorSLDIfNewerByMS = 600000;

        // The problem only exists for styles with an "sld" format (either explicitly or by
        // default).
        if ("sld".equalsIgnoreCase(si.getFormat()) || si.getFormat() == null) {
            String filename = si.getFilename();
            String filenameCss = filename.substring(0, filename.lastIndexOf('.')) + ".css";
            GeoServerDataDirectory dataDir =
                    new GeoServerDataDirectory(getCatalog().getResourceLoader());
            Resource cssResource = dataDir.get(si, filenameCss);

            if (!cssResource.getType().equals(Resource.Type.UNDEFINED)) {
                // If there is an existing CSS file with the style's name, check if it should be
                // recovered.
                Resource sldResource = dataDir.get(si, filename);
                long sldNewerByMs = sldResource.lastmodified() - cssResource.lastmodified();

                if (sldNewerByMs > favorSLDIfNewerByMS) {
                    LOGGER.log(
                            Level.WARNING,
                            "A CSS version of "
                                    + si.getName()
                                    + " has been recovered ("
                                    + filenameCss
                                    + "), but the SLD is more recent by "
                                    + sldNewerByMs
                                    + " ms. The style will be left as an SLD.");
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            "A CSS version of "
                                    + si.getName()
                                    + " has been recovered ("
                                    + filenameCss
                                    + "). The style will be converted to CSS.");
                    si.setFilename(filenameCss);
                    si.setFormat("css");
                    getCatalog().save(si);
                }
            }
        }
    }

    /** Called when a configuration change requires updating an inactive tab */
    protected void configurationChanged() {
        tabbedPanel.visitChildren(StyleEditTabPanel.class, (component, visit) -> {
            if (component instanceof StyleEditTabPanel panel) {
                panel.configurationChanged();
            }
        });
    }

    /** Subclasses must implement to define the submit behavior */
    protected abstract void onStyleFormSubmit();

    protected GSModalWindow getPopup() {
        return popup;
    }

    protected GeoServerDialog getDialog() {
        return dialog;
    }

    protected IModel<LayerInfo> getLayerModel() {
        return layerModel;
    }

    protected CompoundPropertyModel<StyleInfo> getStyleModel() {
        return styleModel;
    }

    public LayerInfo getLayerInfo() {
        return layerModel.getObject();
    }

    public StyleInfo getStyleInfo() {
        return styleModel.getObject();
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
    // Make sure child tabs can see this
    @Override
    protected boolean isAuthenticatedAsAdmin() {
        return super.isAuthenticatedAsAdmin();
    }

    @Override
    protected Catalog getCatalog() {
        return super.getCatalog();
    }

    @Override
    protected GeoServer getGeoServer() {
        return super.getGeoServer();
    }

    @Override
    protected GeoServerApplication getGeoServerApplication() {
        return super.getGeoServerApplication();
    }

    private class StyleTabbedPanel extends AjaxTabbedPanel<ITab> {
        public StyleTabbedPanel(List<ITab> tabs) {
            super("context", tabs);
        }

        @Override
        protected String getTabContainerCssClass() {
            return "tab-row tab-row-compact";
        }

        @Override
        protected WebMarkupContainer newLink(String linkId, final int index) {
            /*
             * Use a submit link here in order to save the state of the current tab to the model
             * setDefaultFormProcessing(false) is used so that we do not do a full submit
             * (with validation + saving to the catalog)
             */
            AjaxSubmitLink link = new AjaxSubmitLink(linkId) {
                @Serial
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget target) {
                    if (getLayerInfo() == null || getLayerInfo().getId() == null) {
                        switch (index) {
                            case 1:
                                tabbedPanel.error("Cannot show Publishing options: No Layers available.");
                                addFeedbackPanels(target);
                                return;
                            case 2:
                                tabbedPanel.error("Cannot show Layer Preview: No Layers available.");
                                addFeedbackPanels(target);
                                return;
                            case 3:
                                tabbedPanel.error("Cannot show Attribute Preview: No Layers available.");
                                addFeedbackPanels(target);
                                return;
                            default:
                                break;
                        }
                    }

                    setSelectedTab(index);
                    target.add(tabbedPanel);
                }
            };
            link.setDefaultFormProcessing(false);
            return link;
        }
    }

    private class InsertImageButton implements CodeMirrorEditor.CustomButtonAction {
        @Override
        public void onClick(AjaxRequestTarget target) {
            String input = editor.getInput();
            dialog.setTitle(new ParamResourceModel("insertImage", AbstractStylePage.this.getPage()));
            dialog.setInitialWidth(385);
            dialog.setInitialHeight(175);

            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                private ChooseImagePanel imagePanel;

                @Override
                protected Component getContents(String id) {
                    return imagePanel = new ChooseImagePanel(
                            id,
                            styleModel.getObject().getWorkspace(),
                            styleHandler().imageExtensions());
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    String imageFileName = imagePanel.getChoice();
                    if (Strings.isEmpty(imageFileName)) {
                        FileUpload fu = imagePanel.getFileUpload();
                        imageFileName = fu.getClientFileName();
                        int teller = 0;
                        GeoServerDataDirectory dd =
                                GeoServerApplication.get().getBeanOfType(GeoServerDataDirectory.class);
                        Resource res = dd.getStyles(styleModel.getObject().getWorkspace(), imageFileName);
                        while (Resources.exists(res)) {
                            imageFileName = getImageFileName(fu, ++teller);
                            res = dd.getStyles(styleModel.getObject().getWorkspace(), imageFileName);
                        }
                        try (InputStream is = fu.getInputStream()) {
                            try (OutputStream os = res.out()) {
                                IOUtils.copy(is, os);
                            }
                        } catch (IOException e) {
                            error(e.getMessage());
                            target.add(imagePanel.getFeedback());
                            return false;
                        }
                    }
                    String code = StringEscapeUtils.escapeEcmaScript(imageFileName);
                    code = styleHandler().insertImageCode(code, input);
                    target.appendJavaScript("replaceSelection('" + code + "');");
                    return true;
                }

                private String getImageFileName(FileUpload fu, int tellerParam) {
                    return FilenameUtils.getBaseName(fu.getClientFileName())
                            + "."
                            + tellerParam
                            + "."
                            + FilenameUtils.getExtension(fu.getClientFileName());
                }

                @Override
                public void onError(AjaxRequestTarget target, Form<?> form) {
                    target.add(imagePanel.getFeedback());
                }
            });
        }
    }

    private class ChooseColorButton implements CodeMirrorEditor.CustomButtonAction {
        @Override
        public void onClick(AjaxRequestTarget target) {
            IRequestParameters queryParameters = target.getPage().getRequest().getQueryParameters();
            StringValue cmSelection = queryParameters.getParameterValue("cmSelection");
            String defaultColor;
            if (cmSelection != null) {
                defaultColor = cmSelection.toString();
            } else {
                defaultColor = "#000";
            }
            dialog.setTitle(new ParamResourceModel("chooseColor", AbstractStylePage.this.getPage()));
            dialog.setInitialWidth(410);
            dialog.setInitialHeight(270);

            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                private ChooseColorPanel chooserPanel;

                @Override
                protected Component getContents(String id) {
                    return chooserPanel = new ChooseColorPanel(id, defaultColor);
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    String chosenColor = chooserPanel.chooser.getModelObject();
                    if (chosenColor != null) {
                        // it's easy to double click a color in the editor,
                        // but it
                        // won't select the #, work around that to allow a
                        // seamless
                        // editing experience
                        if (HEX_COLOR.matcher("#" + defaultColor).matches() && chosenColor.startsWith("#")) {
                            chosenColor = chosenColor.substring(1);
                        }
                        target.appendJavaScript("replaceSelection('" + chosenColor + "');");
                    }
                    return true;
                }
            });
        }
    }
}
