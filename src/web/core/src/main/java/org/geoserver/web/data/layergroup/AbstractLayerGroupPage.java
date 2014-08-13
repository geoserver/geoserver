/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.publish.LayerGroupConfigurationPanel;
import org.geoserver.web.publish.LayerGroupConfigurationPanelInfo;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Handles layer group
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public abstract class AbstractLayerGroupPage extends GeoServerSecuredPage {

    public static final String GROUP = "group";
    IModel<LayerGroupInfo> lgModel;
    EnvelopePanel envelopePanel;
    LayerGroupEntryPanel lgEntryPanel;
    String layerGroupId;
    protected RootLayerEntryPanel rootLayerPanel;    
    private ListView<LayerGroupConfigurationPanelInfo> extensionPanels;
    
    /**
     * Subclasses must call this method to initialize the UI for this page 
     * @param layerGroup
     */
    protected void initUI(LayerGroupInfo layerGroup) {
        this.returnPageClass = LayerGroupPage.class;
        lgModel = new LayerGroupDetachableModel( layerGroup );
        layerGroupId = layerGroup.getId();

        Form form = new Form( "form", new CompoundPropertyModel( lgModel ) ) {
            @Override
            public IConverter getConverter(Class<?> type) {
                if (LayerInfo.class.isAssignableFrom(type)) {
                    return new LayerInfoConverter();
                } else if (StyleInfo.class.isAssignableFrom(type)) {
                    return new StyleInfoConverter(); 
                } else {
                    return super.getConverter(type);
                }
            }
        };
        
        add(form);

        final WebMarkupContainer rootLayerPanelContainer = new WebMarkupContainer("rootLayerContainer");
        rootLayerPanelContainer.setOutputMarkupId(true);
        form.add(rootLayerPanelContainer);        
        
        rootLayerPanel = new RootLayerEntryPanel("rootLayer", form, layerGroup.getWorkspace());
        rootLayerPanelContainer.add(rootLayerPanel);

        updateRootLayerPanel(layerGroup.getMode());
        
        TextField name = new TextField("name");
        name.setRequired(true);
        //JD: don't need this, this is validated at the catalog level
        //name.add(new GroupNameValidator());
        form.add(name);

        final DropDownChoice<LayerGroupInfo.Mode> modeChoice = new DropDownChoice<LayerGroupInfo.Mode>("mode", new LayerGroupModeModel(), new LayerGroupModeChoiceRenderer());
        modeChoice.setNullValid(false);
        modeChoice.setRequired(true);
        modeChoice.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                LayerGroupInfo.Mode mode = modeChoice.getModelObject();
                updateRootLayerPanel(mode);
                target.addComponent(rootLayerPanelContainer);
            }
        });
        
        form.add(modeChoice);
        
        form.add(new TextField("title"));
        form.add(new TextArea("abstract"));
        
        DropDownChoice<WorkspaceInfo> wsChoice = 
                new DropDownChoice("workspace", new WorkspacesModel(), new WorkspaceChoiceRenderer());
        wsChoice.setNullValid(true);
        if (!isAuthenticatedAsAdmin()) {
            wsChoice.setNullValid(false);
            wsChoice.setRequired(true);
        }

        form.add(wsChoice);

        //bounding box
        form.add(envelopePanel = new EnvelopePanel( "bounds" )/*.setReadOnly(true)*/);
        envelopePanel.setRequired(true);
        envelopePanel.setCRSFieldVisible(true);
        envelopePanel.setCrsRequired(true);
        envelopePanel.setOutputMarkupId( true );
        
        form.add(new GeoServerAjaxFormLink( "generateBounds") {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                // build a layer group with the current contents of the group
                LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
                for ( LayerGroupEntry entry : lgEntryPanel.getEntries() ) {
                    lg.getLayers().add(entry.getLayer());
                    lg.getStyles().add(entry.getStyle());
                }
                
                try {
                    // grab the eventually manually inserted 
                    CoordinateReferenceSystem crs = envelopePanel.getCoordinateReferenceSystem();
                     
                    if ( crs != null ) {
                        //ensure the bounds calculated in terms of the user specified crs
                        new CatalogBuilder( getCatalog() ).calculateLayerGroupBounds( lg, crs );
                    }
                    else {
                        //calculate from scratch
                        new CatalogBuilder( getCatalog() ).calculateLayerGroupBounds( lg );
                    }
                    
                    envelopePanel.setModelObject( lg.getBounds() );
                    target.addComponent( envelopePanel );
                    
                } 
                catch (Exception e) {
                    throw new WicketRuntimeException( e );
                }
            }
        });
        
        form.add(lgEntryPanel = new LayerGroupEntryPanel( "layers", layerGroup ));
        
        //Add panels contributed through extension point
        form.add(extensionPanels = extensionPanels());
        
        form.add(saveLink());
        form.add(cancelLink());
    }

    private void updateRootLayerPanel(LayerGroupInfo.Mode mode) {
        rootLayerPanel.setEnabled(LayerGroupInfo.Mode.EO.equals(mode));   
        rootLayerPanel.setVisible(LayerGroupInfo.Mode.EO.equals(mode));
    }    
    
    private ListView<LayerGroupConfigurationPanelInfo> extensionPanels() {

        final GeoServerApplication gsapp = getGeoServerApplication();
        final List<LayerGroupConfigurationPanelInfo> extensions;
        extensions = gsapp.getBeansOfType(LayerGroupConfigurationPanelInfo.class);

        ListView<LayerGroupConfigurationPanelInfo> list;
        list = new ListView<LayerGroupConfigurationPanelInfo>("contributedPanels", extensions) {

            @Override
            protected void populateItem(ListItem<LayerGroupConfigurationPanelInfo> item) {
                final LayerGroupConfigurationPanelInfo panelInfo = item.getModelObject();
                try {
                    LayerGroupConfigurationPanel panel;
                    Class<LayerGroupConfigurationPanel> componentClass;
                    Constructor<? extends LayerGroupConfigurationPanel> constructor;

                    componentClass = panelInfo.getComponentClass();
                    constructor = componentClass.getConstructor(String.class, IModel.class);
                    panel = constructor.newInstance("content", lgModel);
                    item.add(panel);
                } catch (Exception e) {
                    throw new WicketRuntimeException(
                            "Failed to add pluggable layergroup configuration panels", e);
                }
            }
        };
        return list;
    }

    private Component cancelLink() {
        return new AjaxLink<String>("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn();
            }
        };
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save"){
            @Override
            public void onSubmit() {
                // validation
                if(lgEntryPanel.getEntries().size() == 0) {
                    error((String) new ParamResourceModel("oneLayerMinimum", getPage()).getObject());
                    return;
                }

                LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();

                if (!LayerGroupInfo.Mode.EO.equals(lg.getMode())) {
                    lg.setRootLayer(null);
                    lg.setRootLayerStyle(null);
                } else {
                    if (lg.getRootLayerStyle() == null && lg.getRootLayer() != null) {
                        lg.setRootLayerStyle(lg.getRootLayer().getDefaultStyle());
                    }
                }
                
                // update the layer group entries
                lg.getLayers().clear();
                lg.getStyles().clear();
                for ( LayerGroupEntry entry : lgEntryPanel.getEntries() ) {
                    lg.getLayers().add(entry.getLayer());
                    lg.getStyles().add(entry.getStyle());
                }

                try {
                    AbstractLayerGroupPage.this.save();
                }
                catch(Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Error adding/modifying layer group.", e);    
                }
                
            }
        };
    }
    
    private final void save() {
        onSubmit();
        this.extensionPanels.visitChildren(LayerGroupConfigurationPanel.class,
                new IVisitor<LayerGroupConfigurationPanel>() {
                    @Override
                    public Object component(LayerGroupConfigurationPanel extensionPanel) {
                        extensionPanel.save();
                        return CONTINUE_TRAVERSAL;
                    }
                });
    }
    
    /**
     * Subclasses 
     */
    protected abstract void onSubmit();
    
    class GroupNameValidator extends AbstractValidator {

        @Override
        protected void onValidate(IValidatable validatable) {
            String name = (String) validatable.getValue();
            LayerGroupInfo other = getCatalog().getLayerGroupByName(name);
            if(other != null && (layerGroupId == null || !other.getId().equals(layerGroupId))) {
                error(validatable, "duplicateGroupNameError", Collections.singletonMap("name", name));
            }
        }
        
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
