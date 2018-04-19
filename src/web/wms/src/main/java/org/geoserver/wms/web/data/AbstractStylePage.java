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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.FileSystemResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.styling.StyledLayerDescriptor;
import org.xml.sax.SAXParseException;

/**
 * Base page for creating/editing styles
 * <p>
 * WARNING: one crucial aspect of this page is its ability to not loose edits when one switches from
 * one tab to the other. I did not find any effective way to unit test this, so _please_, if you do
 * modify anything in this class (especially the models), manually retest that the edits are not
 * lost on tab switch.
 */
@SuppressWarnings("serial")
public abstract class AbstractStylePage extends GeoServerSecuredPage {
    
    class ChooseImagePanel extends Panel {
        private WorkspaceInfo ws;
        
        public ChooseImagePanel(String id, WorkspaceInfo ws) {
            super(id);   
            this.ws = ws;
        }
        
        @Override
        protected void onInitialize() {
            super.onInitialize();
            
            add(new FeedbackPanel("feedback").setOutputMarkupId(true));
            
            SortedSet<String> imageSet = new TreeSet<String>();
            GeoServerDataDirectory dd =
                    GeoServerApplication.get().getBeanOfType(GeoServerDataDirectory.class);
            for (Resource r : dd.getStyles(ws).list()) {
                if (ArrayUtils.contains(styleHandler().imageExtensions(), 
                        FilenameUtils.getExtension(r.name()).toLowerCase())) {
                    imageSet.add(r.name());
                }
            }
            
            FileUploadField upload = new FileUploadField("upload",
                    new Model<ArrayList<FileUpload>>());
            
            Model<String> imageModel = new Model<String>();
            DropDownChoice<String> image = new DropDownChoice<String>("image", 
                    imageModel, 
                    new ArrayList<String>(imageSet));
            
            URI stylesDir = dd.getStyles(ws).dir().toURI();
            
            Image display = new Image("display", new IModel<ResourceReference>() {
                
                @Override
                public ResourceReference getObject() {
                    if (imageModel.getObject() != null) {
                        try {
                            return new FileSystemResourceReference(imageModel.getObject(),
                                    FileSystemResourceReference.getPath(stylesDir.resolve(imageModel.getObject())));
                        } catch (IOException | URISyntaxException e) {
                            LOGGER.log(Level.WARNING, e.getMessage(), e);
                            return null;
                        }
                    } else {
                        return null;
                    }
                }

                @Override public void setObject(ResourceReference object) {}

                @Override public void detach() {}
                
            });
            display.setOutputMarkupPlaceholderTag(true)
                .setVisible(false);
            
            image.setNullValid(true).setOutputMarkupId(true)
                .add(new OnChangeAjaxBehavior(){
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        upload.setModelObject(null);
                        display.setVisible(image.getModelObject() != null);
                        target.add(upload);
                        target.add(display);
                    }                        
                });
            
            upload.setOutputMarkupId(true)
                .add(new OnChangeAjaxBehavior(){
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    image.setModelObject(null);
                    display.setVisible(false);
                    target.add(image);
                    target.add(display);
                }                        
            });
            
           add(image);
           add(display);
           add(upload);
           
           findParent(Form.class).add(new AbstractFormValidator() {               
                @Override
                 public FormComponent<?>[] getDependentFormComponents() {
                     return new FormComponent<?>[] {image, upload};
                 }
     
                 @Override
                 public void validate(Form<?> form) {
                     if (image.getConvertedInput() == null && 
                             (upload.getConvertedInput() == null ||
                             upload.getConvertedInput().isEmpty())) {
                         form.error(new ParamResourceModel("missingImage", getPage()).getString());
                     }
                 }
             });
        }
        
        public String getChoice() {
            return get("image").getDefaultModelObjectAsString();
        }

        public FileUpload getFileUpload() {
            return ((FileUploadField) get("upload")).getFileUpload();
        }
        
        public FeedbackPanel getFeedback() {
            return (FeedbackPanel) get("feedback");
        }
    }

    protected Form<StyleInfo> styleForm;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    protected CodeMirrorEditor editor;
    
    protected ModalWindow popup;

    protected CompoundPropertyModel<StyleInfo> styleModel;
    protected IModel<LayerInfo> layerModel;

    String rawStyle;

    public AbstractStylePage() {
    }

    public AbstractStylePage(StyleInfo style) {
        recoverCssStyle(style);
        initPreviewLayer(style);
        initUI(style);
    }
    protected void initPreviewLayer(StyleInfo style) {
        Catalog catalog = getCatalog();
        List<LayerInfo> layers;
        
        //Try getting the first layer associated with this style
        if (style != null) {
            layers = catalog.getLayers(style);
            if (layers.size() > 0) {
                layerModel = new Model<LayerInfo>(layers.get(0));
                return;
            }
        }
        
        //Try getting the first layer in the default store in the default workspace
        WorkspaceInfo defaultWs = catalog.getDefaultWorkspace();
        if (defaultWs != null) {
            DataStoreInfo defaultStore = catalog.getDefaultDataStore(defaultWs);
            if (defaultStore != null) {
                List<ResourceInfo> resources = catalog.getResourcesByStore(defaultStore, ResourceInfo.class);
                for (ResourceInfo resource : resources) {
                    layers = catalog.getLayers(resource);
                    if (layers.size() > 0) {
                        layerModel = new Model<LayerInfo>(layers.get(0));
                        return;
                    }
                }
            }
        }
        
        //Try getting the first layer returned by the catalog
        layers = catalog.getLayers();
        if (layers.size() > 0) {
            layerModel = new Model<LayerInfo>(layers.get(0));
            return;
        }
        
        //If none of these succeeded, return an empty model
        layerModel = new Model<LayerInfo>(new LayerInfoImpl());
    }
    
    protected void initUI(StyleInfo style) {
        /* init model */
        if (style == null) {
            styleModel = new CompoundPropertyModel<StyleInfo>(getCatalog().getFactory().createStyle());
            styleModel.getObject().setName("");
            styleModel.getObject().setLegend(getCatalog().getFactory().createLegend());
        } else {
            if (style.getLegend() == null) {
                style.setLegend(getCatalog().getFactory().createLegend());
            }
            styleModel = new CompoundPropertyModel<StyleInfo>(style);
        }
        
        /* init main form */
        styleForm = new Form<StyleInfo>("styleForm", styleModel) {
            @Override
            protected void onSubmit() {
                onStyleFormSubmit();
                super.onSubmit();
            }
        };
        add(styleForm);
        styleForm.setMultiPart(true);
        
        /* init popup */
        popup = new ModalWindow("popup");
        styleForm.add(popup);
        /* init tabs */
        List<ITab> tabs = new ArrayList<ITab>();
        
        //Well known tabs
        PanelCachingTab dataTab = new PanelCachingTab(new AbstractTab(new Model<String>("Data")) {

            public Panel getPanel(String id) {
                return new StyleAdminPanel(id, AbstractStylePage.this);
            }
        });
        
        PanelCachingTab publishingTab = new PanelCachingTab(new AbstractTab(new Model<String>("Publishing")) {
            private static final long serialVersionUID = 4184410057835108176L;

            public Panel getPanel(String id) {
                return new LayerAssociationPanel(id, AbstractStylePage.this);
            };
        });
        
        PanelCachingTab previewTab = new PanelCachingTab(new AbstractTab(new Model<String>("Layer Preview")) {

            public Panel getPanel(String id) {
                return new OpenLayersPreviewPanel(id, AbstractStylePage.this);
            }
        });

        PanelCachingTab attributeTab = new PanelCachingTab(new AbstractTab(new Model<String>("Layer Attributes")) {
            private static final long serialVersionUID = 4184410057835108176L;

            public Panel getPanel(String id) {
                try {
                    return new LayerAttributePanel(id, AbstractStylePage.this);
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }
            };
        });
        //If style is null, this is a new style.
        //If so, we want to disable certain tabs
        tabs.add(dataTab);
        if (style != null) {
            tabs.add(publishingTab);
            tabs.add(previewTab);
            tabs.add(attributeTab);
        }

        //Dynamic tabs
        List<StyleEditTabPanelInfo> tabPanels = getGeoServerApplication().getBeansOfType(StyleEditTabPanelInfo.class);
        
        // sort the tabs based on order
        Collections.sort(tabPanels, new Comparator<StyleEditTabPanelInfo>() {
            public int compare(StyleEditTabPanelInfo o1, StyleEditTabPanelInfo o2) {
                Integer order1 = o1.getOrder() >= 0 ? o1.getOrder() : Integer.MAX_VALUE;
                Integer order2 = o2.getOrder() >= 0 ? o2.getOrder() : Integer.MAX_VALUE;

                return order1.compareTo(order2);
            }
        });
        // instantiate tab panels and add to tabs list
        for (StyleEditTabPanelInfo tabPanelInfo : tabPanels) {
            String titleKey = tabPanelInfo.getTitleKey();
            IModel<String> titleModel = null;
            if (tabPanelInfo.isEnabledOnNew() || style != null) {
                if (titleKey != null) {
                    titleModel = new org.apache.wicket.model.ResourceModel(titleKey);
                } else {
                    titleModel = new Model<String>(tabPanelInfo.getComponentClass().getSimpleName());
                }
                
                final Class<StyleEditTabPanel> panelClass = tabPanelInfo.getComponentClass();
                
                tabs.add(new AbstractTab(titleModel) {
                    private static final long serialVersionUID = -6637277497986497791L;
                    @Override
                    public Panel getPanel(String panelId) {
                        StyleEditTabPanel tabPanel;
                        try {
                            tabPanel = panelClass.getConstructor(String.class, IModel.class)
                                    .newInstance(panelId, styleModel);
                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                        return tabPanel;
                    }
                });
            }
        }
        
        tabbedPanel = new AjaxTabbedPanel<ITab>("context", tabs) {
            protected String getTabContainerCssClass()
            {
                return "tab-row tab-row-compact";
            }
            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                /*
                 * Use a submit link here in order to save the state of the current tab to the model
                 * setDefaultFormProcessing(false) is used so that we do not do a full submit 
                 * (with validation + saving to the catalog)
                 */
                AjaxSubmitLink link =  new AjaxSubmitLink(linkId) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
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
        };
        
        styleForm.add(tabbedPanel);
        
        /* init editor */
        styleForm.add(editor = new CodeMirrorEditor("styleEditor", styleHandler()
                .getCodeMirrorEditMode(), new PropertyModel<String>(this, "rawStyle")));
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        editor.setRequired(true);
        styleForm.add(editor);
                
        //insert picture button
        GeoServerDialog dialog = new GeoServerDialog("dialog");
        dialog.setOutputMarkupId(true);
        add(dialog);
        editor.addCustomButton(new ParamResourceModel("insertImage", getPage()).getString(), "button-picture",
                new CodeMirrorEditor.CustomButtonAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setTitle(new ParamResourceModel("insertImage", getPage()));
                        dialog.setInitialWidth(385);
                        dialog.setInitialHeight(175);
                        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                            private ChooseImagePanel imagePanel;

                            @Override
                            protected Component getContents(String id) {
                                return imagePanel = new ChooseImagePanel(id,
                                        styleModel.getObject().getWorkspace());
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
                                    Resource res = dd.getStyles(style.getWorkspace(), imageFileName);
                                    while (Resources.exists(res)) {
                                        imageFileName = FilenameUtils.getBaseName(fu.getClientFileName())
                                                + "." + (++teller) + "." + 
                                                FilenameUtils.getExtension(fu.getClientFileName());
                                        res = dd.getStyles(style.getWorkspace(), imageFileName);
                                    }
                                    try(InputStream is = fu.getInputStream()) {
                                        try(OutputStream os = res.out()) {
                                            IOUtils.copy(is, os);                                            
                                        }                                        
                                    } catch (IOException e) {
                                        error(e.getMessage());
                                        target.add(imagePanel.getFeedback());
                                        return false;
                                    }
                                }
                                target.appendJavaScript(
                                        "replaceSelection('" + 
                                                styleHandler().insertImageCode(imageFileName) 
                                                + "');");
                                return true;
                            }
                            
                            @Override
                            public void onError(AjaxRequestTarget target, Form<?> form) {
                                target.add(imagePanel.getFeedback());
                            }

                        });                     
                    }
        });
        
        add(validateLink());
        add(new AjaxSubmitLink("apply", styleForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                //If we have a new style, go to the edit page
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
                //Update preview if we are on the preview tab
                if (style != null && tabbedPanel.getSelectedTab() == 2) {
                    tabbedPanel.visitChildren(StyleEditTabPanel.class, (component, visit) -> {
                        if (component instanceof OpenLayersPreviewPanel) {
                            OpenLayersPreviewPanel previewPanel = (OpenLayersPreviewPanel) component;
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
            protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                // Re-initialize the Legend model object, if it is null.
                if (styleModel.getObject().getLegend() == null) {
                    styleModel.getObject().setLegend(getCatalog().getFactory().createLegend());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }
        });
        add(new AjaxSubmitLink("submit", styleForm) {
            @Override
            protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                if (form.hasError()) {
                    addFeedbackPanels(target);
                } else {
                    doReturn(StylePage.class);
                }
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }
        });
        Link<StylePage> cancelLink = new Link<StylePage>("cancel") {
            @Override
            public void onClick() {
                doReturn(StylePage.class);
            }
        };
        add(cancelLink);
        
    }
    
    StyleHandler styleHandler() {
        String format = styleModel.getObject().getFormat();
        return Styles.handler(format);
    }

    Component validateLink() {
        return new GeoServerAjaxFormLink("validate", styleForm) {
            
            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
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
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(editor.getSaveDecorator());
            }
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
            final String style = editor.getInput();
            ByteArrayInputStream input = new ByteArrayInputStream(style.getBytes());
            List<Exception> validationErrors = styleHandler().validate(input, null, getCatalog().getResourcePool().getEntityResolver());
            input = new ByteArrayInputStream(style.getBytes());
            StyledLayerDescriptor sld = styleHandler().parse(input, null, null, getCatalog().getResourcePool().getEntityResolver());
            //If there are more than one layers, assume this is a style group and validate accordingly.
            if (sld.getStyledLayers().length > 1) {
                validationErrors.addAll(SLDNamedLayerValidator.validate(getCatalog(), sld));
            }
            return validationErrors;
        } catch( Exception e ) {
            return Arrays.asList( e );
        }
    }

    Reader readFile(StyleInfo style) throws IOException {
        ResourcePool pool = getCatalog().getResourcePool();
        return pool.readStyle(style);
    }
    
    public void setRawStyle(Reader in) throws IOException {
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

        this.rawStyle = builder.toString();
        editor.setModelObject(rawStyle);
        in.close();
    }

    /**
     * Check for an original CSS version of the style created by the old CSS extension (pre-pluggable styles). If a CSS style is found, recover it if
     * the derived SLD has not subsequently been manually edited.
     * 
     * The recovery is accomplished by updating the catalog to point to the original CSS file, and changing the style's format to "css".
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
            return;
        }

        // Use this tolerance to prevent erasing an SLD that was manually edited after being
        // generated from a CSS file. (Generated SLDs will always be newer than the CSS).
        long favorSLDIfNewerByMS = 600000;

        // The problem only exists for styles with an "sld" format (either explicitly or by default).
        if ("sld".equalsIgnoreCase(si.getFormat())) {
            String filename = si.getFilename();
            String filenameCss = filename.substring(0, filename.lastIndexOf('.')) + ".css";
            GeoServerDataDirectory dataDir = new GeoServerDataDirectory(
                    getCatalog().getResourceLoader());
            Resource cssResource = dataDir.get(si, filenameCss);

            if (!cssResource.getType().equals(Resource.Type.UNDEFINED)) {
                // If there is an existing CSS file with the style's name, check if it should be recovered.
                Resource sldResource = dataDir.get(si, filename);
                long sldNewerByMs = sldResource.lastmodified() - cssResource.lastmodified();

                if (sldNewerByMs > favorSLDIfNewerByMS) {
                    LOGGER.log(Level.WARNING,
                            "A CSS version of " + si.getName() + " has been recovered ("
                                    + filenameCss + "), but the SLD is more recent by "
                                    + sldNewerByMs + " ms. The style will be left as an SLD.");
                } else {
                    LOGGER.log(Level.WARNING,
                            "A CSS version of " + si.getName() + " has been recovered ("
                                    + filenameCss + "). The style will be converted to CSS.");
                    si.setFilename(filenameCss);
                    si.setFormat("css");
                    getCatalog().save(si);
                }
            }
        }
    }

    /**
     * Called when a configuration change requires updating an inactive tab
     */
    protected void configurationChanged() {
        tabbedPanel.visitChildren(StyleEditTabPanel.class, (component, visit) -> {
            if (component instanceof StyleEditTabPanel) {
                ((StyleEditTabPanel) component).configurationChanged();
            }
        });
    }

    /**
     * Subclasses must implement to define the submit behavior
     */
    protected abstract void onStyleFormSubmit();
    
    protected ModalWindow getPopup() {
        return popup;
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
    //Make sure child tabs can see this
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
}
