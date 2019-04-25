/* (c) 2016 - 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.StyleType;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphicBuilder;
import org.geoserver.wms.web.publish.StyleChoiceRenderer;
import org.geoserver.wms.web.publish.StyleTypeChoiceRenderer;
import org.geoserver.wms.web.publish.StyleTypeModel;
import org.geoserver.wms.web.publish.StylesModel;
import org.geotools.styling.Style;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/**
 * Style page tab for displaying editing the style metadata. Includes style upload and generation
 * functionality. Delegates to {@link ExternalGraphicPanel} for editing the legend.
 */
public class StyleAdminPanel extends StyleEditTabPanel {
    private static final long serialVersionUID = -2443344473474977026L;
    private static final Logger LOGGER = Logging.getLogger(StyleAdminPanel.class);

    protected TextField<String> nameTextField;

    protected DropDownChoice<WorkspaceInfo> wsChoice;

    protected DropDownChoice<String> formatChoice;
    protected MarkupContainer formatReadOnlyMessage;

    protected WebMarkupContainer legendContainer;
    protected ExternalGraphicPanel legendPanel;
    protected Image legendImg;

    protected DropDownChoice<StyleType> templates;
    protected AjaxSubmitLink generateLink;

    protected DropDownChoice<StyleInfo> styles;
    protected AjaxSubmitLink copyLink;

    protected FileUploadField fileUploadField;
    protected AjaxSubmitLink uploadLink;

    transient BufferedImage legendImage;

    public StyleAdminPanel(String id, AbstractStylePage parent) {
        super(id, parent);
        initUI(parent.getStyleModel());

        if (stylePage instanceof StyleEditPage) {
            // global styles only editable by full admin
            if (!stylePage.isAuthenticatedAsAdmin()
                    && parent.getStyleInfo().getWorkspace() == null) {
                nameTextField.setEnabled(false);
                uploadLink.setEnabled(false);
            }
            if (StylePage.isDefaultStyle(getStylePage().getStyleInfo())) {
                nameTextField.setEnabled(false);
                wsChoice.setEnabled(false);
            }
            // format only settable upon creation
            formatChoice.setEnabled(false);
            formatReadOnlyMessage.setVisible(true);
        }
    }

    public void initUI(CompoundPropertyModel<StyleInfo> styleModel) {

        StyleInfo style = getStylePage().getStyleInfo();

        IModel<String> nameBinding = styleModel.bind("name");

        add(nameTextField = new TextField<String>("name", nameBinding));
        nameTextField.setRequired(true);

        IModel<WorkspaceInfo> wsBinding = styleModel.bind("workspace");
        wsChoice =
                new Select2DropDownChoice<WorkspaceInfo>(
                        "workspace",
                        wsBinding,
                        new WorkspacesModel(),
                        new WorkspaceChoiceRenderer());
        wsChoice.setNullValid(true);
        if (!stylePage.isAuthenticatedAsAdmin()) {
            wsChoice.setNullValid(false);
            wsChoice.setRequired(true);
        }

        // when editing a default style, disallow changing the name
        if (StylePage.isDefaultStyle(style)) {
            nameTextField.add(
                    new IValidator<String>() {
                        String originalName = style.getName();

                        @Override
                        public void validate(IValidatable<String> validatable) {
                            if (originalName != null
                                    && !originalName.equals(validatable.getValue())) {
                                ValidationError error = new ValidationError();
                                error.setMessage("Can't change the name of default styles.");
                                error.addKey("editDefaultStyleNameDisallowed");
                                validatable.error(error);
                            }
                        }
                    });
            wsChoice.add(
                    (IValidator<WorkspaceInfo>)
                            validatable -> {
                                if (validatable.getValue() != null) {
                                    ValidationError error = new ValidationError();
                                    error.setMessage(
                                            "Can't change the workspace of default styles.");
                                    error.addKey("editDefaultStyleWorkspaceDisallowed");
                                    validatable.error(error);
                                }
                            });
        }

        add(wsChoice);

        // disable the workspace toggle on edit if not admin (can only be set upon creation)
        if (stylePage instanceof StyleEditPage && !stylePage.isAuthenticatedAsAdmin()) {
            wsChoice.setEnabled(false);
        }

        IModel<String> formatBinding = styleModel.bind("format");
        formatChoice =
                new Select2DropDownChoice<>(
                        "format",
                        formatBinding,
                        new StyleFormatsModel(),
                        new ChoiceRenderer<String>() {

                            private static final long serialVersionUID = 2064887235303504013L;

                            @Override
                            public String getIdValue(String object, int index) {
                                return object;
                            }

                            @Override
                            public Object getDisplayValue(String object) {
                                return Styles.handler(object).getName();
                            }
                        });
        formatChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = -8372146231225388561L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.appendJavaScript(
                                String.format(
                                        "if (document.gsEditors) { document.gsEditors.editor.setOption('mode', '%s'); }",
                                        stylePage.styleHandler().getCodeMirrorEditMode()));
                    }
                });
        add(formatChoice);

        formatReadOnlyMessage = new WebMarkupContainer("formatReadOnly", new Model<String>());
        formatReadOnlyMessage.setVisible(false);
        add(formatReadOnlyMessage);
        // add the Legend fields
        legendPanel = new ExternalGraphicPanel("legendPanel", styleModel, stylePage.styleForm);
        legendPanel.setOutputMarkupId(true);
        add(legendPanel);
        if (style.getId() != null) {
            try {
                stylePage.setRawStyle(stylePage.readFile(style));
            } catch (IOException e) {
                // ouch, the style file is gone! Register a generic error message
                Session.get()
                        .error(
                                new ParamResourceModel("styleNotFound", this, style.getFilename())
                                        .getString());
            }
        }

        // style generation functionality
        templates =
                new Select2DropDownChoice<StyleType>(
                        "templates",
                        new Model<StyleType>(),
                        new StyleTypeModel(),
                        new StyleTypeChoiceRenderer());
        templates.setOutputMarkupId(true);
        templates.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = -6649152103570059645L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        templates.validate();
                        generateLink.setEnabled(templates.getConvertedInput() != null);
                        target.add(generateLink);
                    }
                });
        add(templates);
        generateLink = generateLink();
        generateLink.setEnabled(false);
        add(generateLink);

        // style copy functionality
        styles =
                new Select2DropDownChoice<>(
                        "existingStyles",
                        new Model<>(),
                        new StylesModel(),
                        new StyleChoiceRenderer());
        styles.setOutputMarkupId(true);
        styles.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = 8098121930876372129L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        styles.validate();
                        copyLink.setEnabled(styles.getConvertedInput() != null);
                        target.add(copyLink);
                    }
                });
        add(styles);
        copyLink = copyLink();
        copyLink.setEnabled(false);
        add(copyLink);

        uploadLink = uploadLink();
        // uploadLink.setEnabled(false);
        add(uploadLink);

        fileUploadField = new FileUploadField("filename");
        // Explicitly set model so this doesn't use the form model
        fileUploadField.setDefaultModel(new Model<String>(""));
        add(fileUploadField);

        add(previewLink());

        legendContainer = new WebMarkupContainer("legendContainer");
        legendContainer.setOutputMarkupId(true);
        add(legendContainer);
        this.legendImg =
                new Image(
                        "legendImg",
                        new AbstractResource() {

                            private static final long serialVersionUID = -6932528694575832606L;

                            @Override
                            protected ResourceResponse newResourceResponse(Attributes attributes) {
                                ResourceResponse rr = new ResourceResponse();
                                rr.setContentType("image/png");
                                rr.setWriteCallback(
                                        new WriteCallback() {
                                            @Override
                                            public void writeData(Attributes attributes)
                                                    throws IOException {
                                                ImageIO.write(
                                                        legendImage,
                                                        "PNG",
                                                        attributes.getResponse().getOutputStream());
                                            }
                                        });
                                return rr;
                            }
                        });
        legendContainer.add(this.legendImg);
        this.legendImg.setVisible(false);
        this.legendImg.setOutputMarkupId(true);
    }

    /**
     * Clears validation messages from form input elements. Called when it is necessary to submit
     * the form without needing to show validation, such as when you are generating a new style
     */
    protected void clearFeedbackMessages() {
        nameTextField.getFeedbackMessages().clear();
        wsChoice.getFeedbackMessages().clear();
        formatChoice.getFeedbackMessages().clear();
        stylePage.editor.getFeedbackMessages().clear();
    }

    protected Component previewLink() {
        return new GeoServerAjaxFormLink("preview", stylePage.styleForm) {

            private static final long serialVersionUID = 7404304424029960594L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                stylePage.editor.processInput();
                wsChoice.processInput();

                clearFeedbackMessages();
                legendImg.setVisible(false);

                // Generate the legend

                // Try External Legend
                URLConnection conn = legendPanel.getExternalGraphic(target, form);
                String onlineResource = legendPanel.getOnlineResource();
                if (onlineResource != null && !onlineResource.isEmpty()) {
                    if (conn != null) {
                        try {
                            legendImage = ImageIO.read(conn.getInputStream());
                            legendImg.setVisible(true);
                        } catch (IOException e) {
                            LOGGER.log(
                                    Level.WARNING, "Failed to render external legend graphic", e);
                            legendContainer.error("Failed to render external legend graphic");
                        }
                    }
                } else {
                    // No external legend, use generated legend
                    GeoServerDataDirectory dd =
                            GeoServerExtensions.bean(
                                    GeoServerDataDirectory.class,
                                    stylePage.getGeoServerApplication().getApplicationContext());
                    StyleInfo si = new StyleInfoImpl(stylePage.getCatalog());
                    String format = stylePage.getStyleInfo().getFormat();
                    si.setFormat(format);
                    String styleName = "tmp" + UUID.randomUUID().toString();
                    String styleFileName =
                            styleName + '.' + Styles.handler(si.getFormat()).getFileExtension();
                    si.setFilename(styleFileName);
                    si.setName(styleName);
                    si.setWorkspace(stylePage.styleModel.getObject().getWorkspace());
                    Resource styleResource = null;
                    try {
                        styleResource = dd.style(si);
                        try (OutputStream os = styleResource.out()) {
                            IOUtils.write(stylePage.editor.getInput(), os, "UTF-8");
                        }
                        // guess the version, the style in the editor might be using one that's
                        // different from the
                        // the one in the
                        Version version =
                                Styles.handler(format).version(stylePage.editor.getInput());
                        si.setFormatVersion(version);
                        Style style = dd.parsedStyle(si);
                        if (style != null) {
                            GetLegendGraphicRequest request = new GetLegendGraphicRequest();
                            request.setLayer(null);
                            request.setStyle(style);
                            request.setStrict(false);
                            Map<String, String> legendOptions = new HashMap<>();
                            legendOptions.put("forceLabels", "on");
                            legendOptions.put("fontAntiAliasing", "true");
                            request.setLegendOptions(legendOptions);
                            BufferedImageLegendGraphicBuilder builder =
                                    new BufferedImageLegendGraphicBuilder();
                            legendImage = builder.buildLegendGraphic(request);
                            legendImg.setVisible(true);
                        }
                    } catch (IOException e) {
                        throw new WicketRuntimeException(e);
                    } catch (Exception e) {
                        legendImg.setVisible(false);
                        legendContainer.error(
                                "Failed to build legend preview. Check to see if the style is valid.");
                        LOGGER.log(Level.WARNING, "Failed to build legend preview", e);
                    } finally {
                        if (styleResource != null) {
                            styleResource.delete();
                        }
                    }
                }
                target.add(legendContainer);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(stylePage.editor.getSaveDecorator());
            }
        };
    }

    protected AjaxSubmitLink generateLink() {
        return new ConfirmOverwriteSubmitLink("generate") {

            private static final long serialVersionUID = 55921414750155395L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                // we need to force validation or the value won't be converted
                templates.processInput();
                nameTextField.processInput();
                wsChoice.processInput();
                StyleType template = (StyleType) templates.getConvertedInput();
                StyleGenerator styleGen = new StyleGenerator(stylePage.getCatalog());
                styleGen.setWorkspace(getStylePage().getStyleInfo().getWorkspace());

                if (template != null) {
                    try {
                        // same here, force validation or the field won't be updated
                        stylePage.editor.reset();
                        stylePage.setRawStyle(
                                new StringReader(
                                        styleGen.generateStyle(
                                                stylePage.styleHandler(),
                                                template,
                                                getStylePage().getStyleInfo().getName())));
                        target.appendJavaScript(
                                String.format(
                                        "if (document.gsEditors) { document.gsEditors.editor.setOption('mode', '%s'); }",
                                        stylePage.styleHandler().getCodeMirrorEditMode()));
                        clearFeedbackMessages();
                    } catch (Exception e) {
                        clearFeedbackMessages();
                        stylePage.editor.getFeedbackMessages().clear();
                        stylePage.error("Errors occurred generating the style");
                        LOGGER.log(Level.WARNING, "Errors occured generating the style", e);
                    }

                    target.add(stylePage);
                }
            }
        };
    }

    protected AjaxSubmitLink copyLink() {
        return new ConfirmOverwriteSubmitLink("copy") {

            private static final long serialVersionUID = -6388040033082157163L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                // we need to force validation or the value won't be converted
                styles.processInput();
                StyleInfo style = (StyleInfo) styles.getConvertedInput();

                if (style != null) {
                    try {
                        // same here, force validation or the field won't be updated
                        stylePage.editor.reset();
                        stylePage.setRawStyle(stylePage.readFile(style));
                        if (formatChoice.isEnabled()) {
                            formatChoice.setModelObject(style.getFormat());
                        }
                        target.appendJavaScript(
                                String.format(
                                        "if (document.gsEditors) { document.gsEditors.editor.setOption('mode', '%s'); }",
                                        stylePage.styleHandler().getCodeMirrorEditMode()));
                        clearFeedbackMessages();
                    } catch (Exception e) {
                        clearFeedbackMessages();
                        stylePage.error(
                                "Errors occurred loading the '" + style.getName() + "' style");
                        LOGGER.log(
                                Level.WARNING,
                                "Errors occurred loading the '" + style.getName() + "' style",
                                e);
                    }
                    target.add(stylePage);
                }
            }
        };
    }

    AjaxSubmitLink uploadLink() {
        return new ConfirmOverwriteSubmitLink("upload", stylePage.styleForm) {

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
                    stylePage.editor.reset();
                    stylePage.setRawStyle(
                            new InputStreamReader(
                                    new ByteArrayInputStream(bout.toByteArray()), "UTF-8"));
                    target.appendJavaScript(
                            String.format(
                                    "if (document.gsEditors) { document.gsEditors.editor.setOption('mode', '%s'); }",
                                    stylePage.styleHandler().getCodeMirrorEditMode()));
                    clearFeedbackMessages();
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                } catch (Exception e) {
                    clearFeedbackMessages();
                    stylePage.error(
                            "Errors occurred uploading the '"
                                    + upload.getClientFileName()
                                    + "' style");
                    LOGGER.log(
                            Level.WARNING,
                            "Errors occurred uploading the '"
                                    + upload.getClientFileName()
                                    + "' style",
                            e);
                }

                // update the style object
                StyleInfo s = getStylePage().getStyleInfo();
                if (s.getName() == null || "".equals(s.getName().trim())) {
                    // set it
                    nameTextField.setModelValue(
                            new String[] {
                                ResponseUtils.stripExtension(upload.getClientFileName())
                            });
                    nameTextField.modelChanged();
                }
                target.add(stylePage);
            }
        };
    }

    class ConfirmOverwriteSubmitLink extends AjaxSubmitLink {

        private static final long serialVersionUID = 2673499149884774636L;

        public ConfirmOverwriteSubmitLink(String id) {
            super(id);
        }

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
                                            new ParamResourceModel("confirmOverwrite", stylePage)
                                                    .getString();
                                    message = JavaScriptUtils.escapeQuotes(message);
                                    return "var val = attrs.event.view.document.gsEditors ? "
                                            + "attrs.event.view.document.gsEditors."
                                            + stylePage.editor.getTextAreaMarkupId()
                                            + ".getValue() : "
                                            + "attrs.event.view.document.getElementById(\""
                                            + stylePage.editor.getTextAreaMarkupId()
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
}
