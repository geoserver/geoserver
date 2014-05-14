/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.DynamicWebResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphicBuilder;
import org.geoserver.wms.web.publish.StyleChoiceRenderer;
import org.geoserver.wms.web.publish.StylesModel;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.xml.sax.SAXParseException;

/**
 * Base page for creating/editing styles
 */
@SuppressWarnings("serial")
public abstract class AbstractStylePage extends GeoServerSecuredPage {

    protected TextField nameTextField;

    protected FileUploadField fileUploadField;

    protected DropDownChoice styles;

    protected AjaxSubmitLink copyLink;

    protected Form uploadForm;

    protected Form styleForm;

    protected CodeMirrorEditor editor;
    
    String rawSLD;

    private Image legend;

    String lastStyle;

    private WebMarkupContainer legendContainer;

    public AbstractStylePage() {
    }

    public AbstractStylePage(StyleInfo style) {
        initUI(style);
    }

    protected void initUI(StyleInfo style) {
        IModel<StyleInfo> styleModel = new CompoundPropertyModel(style != null ? 
            new StyleDetachableModel(style) : getCatalog().getFactory().createStyle());
        
        styleForm = new Form("form", styleModel) {
            @Override
            protected void onSubmit() {
                super.onSubmit();
                onStyleFormSubmit();
            }
        };
        styleForm.setMarkupId("mainForm");
        add(styleForm);

        styleForm.add(nameTextField = new TextField("name"));
        nameTextField.setRequired(true);
        
        DropDownChoice<WorkspaceInfo> wsChoice = 
            new DropDownChoice("workspace", new WorkspacesModel(), new WorkspaceChoiceRenderer());
        wsChoice.setNullValid(true);
        if (!isAuthenticatedAsAdmin()) {
            wsChoice.setNullValid(false);
            wsChoice.setRequired(true);
        }

        styleForm.add(wsChoice);
        styleForm.add( editor = new CodeMirrorEditor("SLD", new PropertyModel(this, "rawSLD")) );
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        editor.setRequired(true);
        styleForm.add(editor);

        if (style != null) {
            try {
                setRawSLD(readFile(style));
            } catch (IOException e) {
                // ouch, the style file is gone! Register a generic error message
                Session.get().error(new ParamResourceModel("sldNotFound", this, style.getFilename()).getString());
            }
        }

        // style copy functionality
        styles = new DropDownChoice("existingStyles", new Model(), new StylesModel(), new StyleChoiceRenderer());
        styles.setOutputMarkupId(true);
        styles.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                styles.validate();
                copyLink.setEnabled(styles.getConvertedInput() != null);
                target.addComponent(copyLink);
            }
        });
        styleForm.add(styles);
        copyLink = copyLink();
        copyLink.setEnabled(false);
        styleForm.add(copyLink);

        uploadForm = uploadForm(styleForm);
        uploadForm.setMultiPart(true);
        uploadForm.setMaxSize(Bytes.megabytes(1));
        uploadForm.setMarkupId("uploadForm");
        add(uploadForm);

        uploadForm.add(fileUploadField = new FileUploadField("filename"));

        

        add(validateLink());
        add(previewLink());
        Link cancelLink = new Link("cancel") {
            @Override
            public void onClick() {
                doReturn(StylePage.class);
            }
        };
        add(cancelLink);

        legendContainer = new WebMarkupContainer("legendContainer");
        legendContainer.setOutputMarkupId(true);
        add(legendContainer);
        legend = new Image("legend");
        legendContainer.add(legend);
        legend.setVisible(false);
        legend.setOutputMarkupId(true);
        legend.setImageResource(new DynamicWebResource() {

            @Override
            protected ResourceState getResourceState() {
                return new ResourceState() {

                    @Override
                    public byte[] getData() {
                        try {
                            ByteArrayInputStream input = new ByteArrayInputStream(lastStyle
                                    .getBytes());
                            EntityResolverProvider entityResolverProvider = new EntityResolverProvider(
                                    getGeoServer());
                            StyledLayerDescriptor sld = Styles.parse(input,
                                    entityResolverProvider.getEntityResolver());
                            if (sld != null && sld.getStyledLayers().length > 0) {
                                Style style = null;
                                StyledLayer sl = sld.getStyledLayers()[0];
                                if (sl instanceof UserLayer) {
                                    style = ((UserLayer) sl).getUserStyles()[0];
                                } else {
                                    style = ((NamedLayer) sl).getStyles()[0];
                                }

                                GetLegendGraphicRequest request = new GetLegendGraphicRequest();
                                request.setStyle(style);
                                request.setLayer(null);
                                request.setStrict(false);
                                Map<String, String> legendOptions = new HashMap<String, String>();
                                legendOptions.put("forceLabels", "on");
                                legendOptions.put("fontAntiAliasing", "true");
                                request.setLegendOptions(legendOptions);
                                BufferedImageLegendGraphicBuilder builder = new BufferedImageLegendGraphicBuilder();
                                BufferedImage image = builder.buildLegendGraphic(request);

                                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                                ImageIO.write(image, "PNG", bos);
                                return bos.toByteArray();
                            }

                            error("Failed to build legend preview");
                            return null;

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public String getContentType() {
                        return "image/png";
                    }
                };
            }
        });
    }

    Form uploadForm(final Form form) {
        return new Form("uploadForm") {
            @Override
            protected void onSubmit() {
                FileUpload upload = fileUploadField.getFileUpload();
                if (upload == null) {
                    warn("No file selected.");
                    return;
                }
                ByteArrayOutputStream bout = new ByteArrayOutputStream();

                try {
                    IOUtils.copy(upload.getInputStream(), bout);
                    setRawSLD(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray()), "UTF-8"));
                    editor.setModelObject(rawSLD);
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }

                // update the style object
                StyleInfo s = (StyleInfo) form.getModelObject();
                if (s.getName() == null || "".equals(s.getName().trim())) {
                    // set it
                    nameTextField.setModelValue(ResponseUtils.stripExtension(upload
                            .getClientFileName()));
                    nameTextField.modelChanged();
                }
            }
        };
    }
    
    Component validateLink() {
        return new GeoServerAjaxFormLink("validate", styleForm) {
            
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                editor.processInput();
                List<Exception> errors = validateSLD();
                
                if ( errors.isEmpty() ) {
                    form.info( "No validation errors.");
                } else {
                    for( Exception e : errors ) {
                        form.error( sldErrorWithLineNo(e) );
                    }    
                }        
            }
            
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return editor.getSaveDecorator();
            };
        };
    }
    
    Component previewLink() {
        return new GeoServerAjaxFormLink("preview", styleForm) {

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                editor.processInput();
                lastStyle = editor.getInput();

                legend.setVisible(true);
                target.addComponent(legendContainer);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return editor.getSaveDecorator();
            };
        };
    }

    private String sldErrorWithLineNo(Exception e) {
        if (e instanceof SAXParseException) {
            SAXParseException se = (SAXParseException) e;
            return "line " + se.getLineNumber() + ": " + e.getLocalizedMessage();
        }
        String message = e.getLocalizedMessage();
        if(message != null) {
            return message;
        } else {
            return new ParamResourceModel("genericError", this).getString();
        }
    }
    
    List<Exception> validateSLD() {
        try {
            final String sld = editor.getInput();            
            ByteArrayInputStream input = new ByteArrayInputStream(sld.getBytes());
            List<Exception> validationErrors = Styles.validate(input, null);
            return validationErrors;
        } catch( Exception e ) {
            return Arrays.asList( e );
        }
    }

    AjaxSubmitLink copyLink() {
        return new AjaxSubmitLink("copy") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // we need to force validation or the value won't be converted
                styles.processInput();
                StyleInfo style = (StyleInfo) styles.getConvertedInput();

                if (style != null) {
                    try {
                        // same here, force validation or the field won't be udpated
                        editor.reset();
                        setRawSLD(readFile(style));
                    } catch (Exception e) {
                        error("Errors occurred loading the '" + style.getName() + "' style");
                    }
                    target.addComponent(styleForm);
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                    @Override
                    public CharSequence preDecorateScript(CharSequence script) {
                        return "if(event.view.document.gsEditors."
                                + editor.getTextAreaMarkupId()
                                + ".getCode() != '' &&"
                                + "!confirm('"
                                + new ParamResourceModel("confirmOverwrite", AbstractStylePage.this)
                                        .getString() + "')) return false;" + script;
                    }
                };
            }

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

        };
    }

    Reader readFile(StyleInfo style) throws IOException {
        ResourcePool pool = getCatalog().getResourcePool();
        return pool.readStyle(style);
    }
    
    public void setRawSLD(Reader in) throws IOException {
        BufferedReader bin = null;
        if ( in instanceof BufferedReader ) {
            bin = (BufferedReader) in;
        }
        else {
            bin = new BufferedReader( in );
        }
        
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = bin.readLine()) != null ) {
            builder.append(line).append("\n");
        }

        this.rawSLD = builder.toString();
        editor.setModelObject(rawSLD);
        in.close();
    }

    /**
     * Subclasses must implement to define the submit behavior
     */
    protected abstract void onStyleFormSubmit();

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
