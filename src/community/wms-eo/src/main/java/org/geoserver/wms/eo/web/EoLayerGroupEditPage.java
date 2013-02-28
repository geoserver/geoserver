/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layergroup.LayerGroupDetachableModel;
import org.geoserver.web.data.layergroup.LayerGroupEntry;
import org.geoserver.web.data.layergroup.LayerInfoConverter;
import org.geoserver.web.data.layergroup.StyleInfoConverter;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.publish.LayerGroupConfigurationPanel;
import org.geoserver.web.publish.LayerGroupConfigurationPanelInfo;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Edits a layer group
 */
public class EoLayerGroupEditPage extends GeoServerSecuredPage {

    public static final String GROUP = "group";
    public static final String WORKSPACE = "workspace";
    protected IModel<LayerGroupInfo> lgModel;
    private EnvelopePanel envelopePanel;
    private EoLayerGroupEntryPanel lgEntryPanel;
    private BrowseImageLayerEntryPanel browseImageLayerPanel;    
    private BandsLayerEntryPanel bandsLayerPanel;
    private OutlineLayerEntryPanel outlineLayerPanel;
    private ListView<LayerGroupConfigurationPanelInfo> extensionPanels;
    
    
    public EoLayerGroupEditPage(PageParameters parameters) {
        String groupName = parameters.getString(GROUP);
        String wsName = parameters.getString(WORKSPACE);

        LayerGroupInfo lg = wsName != null ? getCatalog().getLayerGroupByName(wsName, groupName) :  
            getCatalog().getLayerGroupByName(groupName);
        
        if (lg == null) {
            error(new ParamResourceModel("LayerGroupEditPage.notFound", this, groupName).getString());
            doReturn(EoLayerGroupPage.class);
            return;
        }
        
        initUI(lg);

        if (!isAuthenticatedAsAdmin()) {
            Form f = (Form) get("form");
    
            // global layer groups only editable by full admin
            if (lg.getWorkspace() == null) {
                // disable all form components but cancel
                disableForm(f);
                
                info(new StringResourceModel("globalLayerGroupReadOnly", this, null).getString());
            }

            // always disable the workspace toggle
            f.get("workspace").setEnabled(false);
        }
    }

    
    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
    
    private final void save() {
        LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();
        getCatalog().save(lg);
        
        this.extensionPanels.visitChildren(LayerGroupConfigurationPanel.class,
                new IVisitor<LayerGroupConfigurationPanel>() {
                    @Override
                    public Object component(LayerGroupConfigurationPanel extensionPanel) {
                        extensionPanel.save();
                        return CONTINUE_TRAVERSAL;
                    }
                });
        
        doReturn();
    }
    
    private void initUI(LayerGroupInfo layerGroup) {
        returnPageClass = EoLayerGroupPage.class;
        lgModel = new LayerGroupDetachableModel(layerGroup);

        Form<LayerGroupInfo> form = new Form<LayerGroupInfo>("form", new CompoundPropertyModel<LayerGroupInfo>(lgModel)) {
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

        browseImageLayerPanel = new BrowseImageLayerEntryPanel("browseImageLayer", form, layerGroup.getWorkspace());
        addPanelToForm(browseImageLayerPanel, "browseImageLayerContainer", form);
        
        bandsLayerPanel = new BandsLayerEntryPanel("bandsLayer", form, layerGroup.getWorkspace());
        addPanelToForm(bandsLayerPanel, "bandsLayerContainer", form);

        outlineLayerPanel = new OutlineLayerEntryPanel("outlineLayer", form, layerGroup.getWorkspace());
        addPanelToForm(outlineLayerPanel, "outlineLayerContainer", form);
        
        TextField<String> name = new TextField<String>("name");
        name.setEnabled(false);
        form.add(name);
        
        form.add(new TextField<String>("title"));
        form.add(new TextArea<String>("abstract"));
        
        DropDownChoice<WorkspaceInfo> wsChoice = 
                new DropDownChoice<WorkspaceInfo>("workspace", new WorkspacesModel(), new WorkspaceChoiceRenderer());
        wsChoice.setNullValid(true);
        if (!isAuthenticatedAsAdmin()) {
            wsChoice.setNullValid(false);
            wsChoice.setRequired(true);
        }

        form.add(wsChoice);

        // bounding box
        form.add(envelopePanel = new EnvelopePanel("bounds"));
        envelopePanel.setRequired(true);
        envelopePanel.setCRSFieldVisible(true);
        envelopePanel.setOutputMarkupId(true);
        form.add(new GeoServerAjaxFormLink("generateBounds") {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                envelopePanel.setModelObject(calculateBounds(lgEntryPanel.getEntries()));
            }
        });
        
        form.add(lgEntryPanel = new EoLayerGroupEntryPanel("layers", layerGroup));

        // Add panels contributed through extension point
        form.add(extensionPanels = extensionPanels());
        
        form.add(saveLink());
        form.add(cancelLink());
    }
    
    private ReferencedEnvelope calculateBounds(List<LayerGroupEntry> entries) {
        // build a tmp layer group with the current contents of the group
        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
        for (LayerGroupEntry entry : lgEntryPanel.getEntries()) {
            lg.getLayers().add(entry.getLayer());
            lg.getStyles().add(entry.getStyle());
        }

        try {
            // grab the eventually manually inserted
            CoordinateReferenceSystem crs = envelopePanel.getCoordinateReferenceSystem();

            if (crs != null) {
                // ensure the bounds calculated in terms of the user specified crs
                new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg, crs);
            } else {
                // calculate from scratch
                new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
            }

            return lg.getBounds();
        } catch (Exception e) {
            throw new WicketRuntimeException(e);
        }            
    }
    
    private void addPanelToForm(Panel panel, String containerId, Form form) {
        WebMarkupContainer panelContainer = new WebMarkupContainer(containerId);
        panelContainer.setOutputMarkupId(true);
        panelContainer.add(panel);        
        form.add(panelContainer);        
    }
    
    private void disableForm(Form f) {
        f.visitChildren(new IVisitor<Component>() {
            @Override
            public Object component(Component c) {
                if (!(c instanceof AbstractLink && "cancel".equals(c.getId()))) {
                    c.setEnabled(false);
                }
                return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
        f.get("save").setVisible(false);
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
                LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();
                
                // update the layer group entries
                lg.getLayers().clear();
                lg.getStyles().clear();
                
                lg.getLayers().add(bandsLayerPanel.getLayer());
                lg.getStyles().add(bandsLayerPanel.getLayerStyle());

                lg.getLayers().add(outlineLayerPanel.getLayer());
                lg.getStyles().add(outlineLayerPanel.getLayerStyle());                
                
                for (LayerGroupEntry entry : lgEntryPanel.getEntries()) {
                    lg.getLayers().add(entry.getLayer());
                    lg.getStyles().add(entry.getStyle());
                }

                try {
                    EoLayerGroupEditPage.this.save();
                } catch (Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Error editing layer group.", e);
                }
            }
        };
    }    
}