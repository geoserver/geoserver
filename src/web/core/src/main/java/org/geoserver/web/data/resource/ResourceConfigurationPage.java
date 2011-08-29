/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.publish.LayerConfigurationPanelInfo;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridGeometry;

/**
 * Page allowing to configure a layer and its resource.
 * <p>
 * The page is completely pluggable, the UI will be made up by scanning the Spring context for
 * implementations of {@link ResourceConfigurationPanel} and {@link LayerConfigurationPanel}.
 * <p>
 * WARNING: one crucial aspect of this page is its ability to not loose edits when one switches from
 * one tab to the other. I did not find any effective way to unit test this, so _please_, if you do
 * modify anything in this class (especially the models), manually retest that the edits are not
 * lost on tab switch.
 */
@SuppressWarnings("serial")
public class ResourceConfigurationPage extends GeoServerSecuredPage {

    public static final String NAME = "name";
    
    public static final String WORKSPACE = "wsName";

    private IModel myResourceModel;

    private IModel myLayerModel;

    private boolean isNew;
    
    public ResourceConfigurationPage(PageParameters parameters) {
        this(parameters.getString(WORKSPACE), parameters.getString(NAME));
    }

    public ResourceConfigurationPage(String workspaceName, String layerName) {
        LayerInfo layer;
        if(workspaceName != null) {
            NamespaceInfo ns = getCatalog().getNamespaceByPrefix(workspaceName);
            if(ns == null) {
                // unlikely to happen, requires someone making modifications on the workspaces
                // with a layer page open in another tab/window
                throw new RuntimeException("Could not find workspace " + workspaceName);
            }
            String nsURI = ns.getURI();
            layer = getCatalog().getLayerByName(new NameImpl(nsURI, layerName));
        } else {
            layer = getCatalog().getLayerByName(layerName);
        }
        
        if(layer == null) {
            error(new ParamResourceModel("ResourceConfigurationPage.notFound", this, layerName).getString());
            setResponsePage(LayerPage.class);
            return;
        }

        
        setup(getCatalog().getResource(layer.getResource().getId(), ResourceInfo.class) , layer);
        this.isNew = false;
        initComponents();
    }

    public ResourceConfigurationPage(ResourceInfo info, boolean isNew) {
        setup(info, getCatalog().getLayers(info).get(0));
        this.isNew = isNew;
        initComponents();
    }

    public ResourceConfigurationPage(LayerInfo info, boolean isNew) {
        setup(info.getResource(), info);
        this.isNew = isNew;
        initComponents();
    }

    private void setup(ResourceInfo resource, LayerInfo layer) {
        layer.setResource(resource);
        myResourceModel = new CompoundPropertyModel(new ResourceModel(resource));
        myLayerModel = new CompoundPropertyModel(new LayerModel(layer));
    }

    private void initComponents() {
        add(new Label("resourcename", getResourceInfo().getPrefixedName()));
        Form theForm = new Form("resource", myResourceModel);
        add(theForm);
        
        List<ITab> tabs = new ArrayList<ITab>();
        
        //add the "well known" tabs
        tabs.add(new AbstractTab(
            new org.apache.wicket.model.ResourceModel("ResourceConfigurationPage.Data")) {
            
            public Panel getPanel(String panelID) {
                return new DataLayerEditTabPanel(panelID, myLayerModel);
            }
        });
        tabs.add(new AbstractTab(
            new org.apache.wicket.model.ResourceModel("ResourceConfigurationPage.Publishing")) {
          
            public Panel getPanel(String panelID) {
                return new PublishingLayerEditTabPanel(panelID, myLayerModel);
            }
        });
        
        //add the tabs contributed via extension point
        List<LayerEditTabPanelInfo> tabPanels = 
            getGeoServerApplication().getBeansOfType(LayerEditTabPanelInfo.class);
        
        //sort the tabs based on order
        Collections.sort(tabPanels, new Comparator<LayerEditTabPanelInfo>() {
            public int compare(LayerEditTabPanelInfo o1, LayerEditTabPanelInfo o2) {
                Integer order1 = o1.getOrder() >= 0 ? o1.getOrder() : Integer.MAX_VALUE;
                Integer order2 = o2.getOrder() >= 0 ? o2.getOrder() : Integer.MAX_VALUE;
                
                return order1.compareTo( order2 );
            }
        });
        
        for (LayerEditTabPanelInfo tabPanel : tabPanels) {
            String titleKey = tabPanel.getTitleKey();
            IModel titleModel = null;
            if (titleKey != null) {
                titleModel = new org.apache.wicket.model.ResourceModel(titleKey);
            }
            else {
                titleModel = new Model(tabPanel.getComponentClass().getSimpleName());
            }
            
            final Class<LayerEditTabPanel> panelClass = tabPanel.getComponentClass(); 
            tabs.add(new AbstractTab(titleModel) {
                
                @Override
                public Panel getPanel(String panelId) {
                    try {
                        return panelClass.getConstructor(String.class, IModel.class)
                            .newInstance(panelId, myLayerModel);
                    } 
                    catch (Exception e) {
                        throw new WicketRuntimeException(e);
                        //LOGGER.log(Level.WARNING, "Error creating resource panel", e);
                    } 
                }
            });
            
        }
        
        // we need to override with submit links so that the various form
        // element
        // will validate and write down into their
        theForm.add(new TabbedPanel("tabs", tabs) {
            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new SubmitLink(linkId) {
                    @Override
                    public void onSubmit() {
                        setSelectedTab(index);
                    }
                };
            }
        });

        theForm.add(saveLink());
        theForm.add(cancelLink());
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                doSave();
            }
        };
    }

    /**
     * Performs the necessary operation on save.
     * <p>
     * This implementation adds the necessary objects to the catalog, respecting the isNew flag, 
     * and calls {@link #onSuccessfulSave()} upon success.
     * </p>
     */
    protected void doSave() {
        try {
            Catalog catalog = getCatalog();
            ResourceInfo resourceInfo = getResourceInfo();
            if (isNew) {
                // updating grid if is a coverage
                if(resourceInfo instanceof CoverageInfo) {
                    // the coverage bounds computation path is a bit more linear, the
                    // readers always return the bounds and in the proper CRS (afaik)
                    CoverageInfo cinfo = (CoverageInfo) resourceInfo;     
                    AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) cinfo.getGridCoverageReader(null, GeoTools.getDefaultHints()); 

                    // get  bounds
                    final ReferencedEnvelope bounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
                    // apply the bounds, taking into account the reprojection policy if need be 
                    final ProjectionPolicy projectionPolicy=resourceInfo.getProjectionPolicy();
                    if (projectionPolicy != ProjectionPolicy.NONE && bounds != null) {
                        // we need to fix the registered grid for this coverage
                        final GridGeometry grid = cinfo.getGrid();
                        cinfo.setGrid(new GridGeometry2D(grid.getGridRange(),grid.getGridToCRS(), resourceInfo.getCRS()));
                    } 
                }
                
                catalog.add(resourceInfo);
                try {
                    catalog.add(getLayerInfo());
                } catch (IllegalArgumentException e) {
                    catalog.remove(resourceInfo);
                    throw e;
                }
            } else {
                ResourceInfo oldState = catalog.getResource(resourceInfo.getId(), ResourceInfo.class);
                catalog.save(resourceInfo);
                try {
                    LayerInfo layer = getLayerInfo();
                    layer.setResource(resourceInfo);
                    catalog.save(layer);
                } catch (IllegalArgumentException e) {
                    catalog.save(oldState);
                    throw e;
                }
            }
            onSuccessfulSave();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error saving layer", e);
            error(e.getMessage());
        }
    }

    private Link cancelLink() {
        return new Link("cancel") {

            @Override
            public void onClick() {
                onCancel();                
            }
        };
    }

    private List<ResourceConfigurationPanelInfo> filterResourcePanels(
            List<ResourceConfigurationPanelInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).canHandle(getResourceInfo())) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

    private List<LayerConfigurationPanelInfo> filterLayerPanels(
            List<LayerConfigurationPanelInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).canHandle(getLayerInfo())) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

    private abstract class ListLayerEditTabPanel extends LayerEditTabPanel {

        public ListLayerEditTabPanel(String id, IModel model) {
            super(id, model);
            
            ListView list = createList("theList");
            
            // do this or die on validation (the form element contents will
            // reset, the edit will be lost)
            list.setReuseItems(true);
            add(list);
        }

        protected abstract ListView createList(String id);

    }
    
    private class DataLayerEditTabPanel extends ListLayerEditTabPanel {

        public DataLayerEditTabPanel(String id, IModel model) {
            super(id, model);
        }

        protected ListView createList(String id) {
            List dataPanels = filterResourcePanels(((GeoServerApplication) getGeoServerApplication())
                    .getBeansOfType(ResourceConfigurationPanelInfo.class));
            ListView dataPanelList = new ListView(id, dataPanels) {
                @Override
                protected void populateItem(ListItem item) {
                    ResourceConfigurationPanelInfo panelInfo = (ResourceConfigurationPanelInfo) item
                            .getModelObject();
                    try {
                        final Class<ResourceConfigurationPanel> componentClass = panelInfo
                                .getComponentClass();
                        final Constructor<ResourceConfigurationPanel> constructor;
                        constructor = componentClass.getConstructor(String.class, IModel.class);
                        ResourceConfigurationPanel panel = constructor.newInstance("content",
                                myResourceModel);
                        item.add((Component) panel);
                    } catch (Exception e) {
                        throw new WicketRuntimeException(
                                "Failed to add pluggable resource configuration panels", e);
                    }
                }
            };
            return dataPanelList;
        }
        
    }
    
    private class PublishingLayerEditTabPanel extends ListLayerEditTabPanel {
        private static final long serialVersionUID = -6575960326680386479L;

        public PublishingLayerEditTabPanel(String id, IModel model) {
            super(id, model);
        }

        @Override
        public ListView createList(String id) {
            List pubPanels = filterLayerPanels(((GeoServerApplication) getGeoServerApplication())
                    .getBeansOfType(LayerConfigurationPanelInfo.class));
            ListView pubPanelList = new ListView(id, pubPanels) {
                @Override
                protected void populateItem(ListItem item) {
                    LayerConfigurationPanelInfo panelInfo = (LayerConfigurationPanelInfo) item
                    .getModelObject();
                    try {
                        LayerConfigurationPanel panel = panelInfo.getComponentClass().getConstructor(
                                String.class, IModel.class).newInstance("content", myLayerModel);
                        item.add((Component) panel);
                    } catch (Exception e) {
                        throw new WicketRuntimeException(
                                "Failed to add pluggable layer configuration panels", e);
                    }
                }
            };
            return pubPanelList;
        }

    }

    /**
     * Returns the {@link ResourceInfo} contained in this page
     * 
     * @return
     */
    public ResourceInfo getResourceInfo() {
        return (ResourceInfo) myResourceModel.getObject();
    }

    /**
     * Returns the {@link LayerInfo} contained in this page
     * 
     * @return
     */
    public LayerInfo getLayerInfo() {
        return (LayerInfo) myLayerModel.getObject();
    }
    
    /**
     * By default brings back the user to LayerPage, subclasses can override this behavior
     */
    protected void onSuccessfulSave() {
        setResponsePage(LayerPage.class);
    }
    
    /**
     * By default brings back the user to LayerPage, subclasses can override this behavior
     */
    protected void onCancel() {
        setResponsePage(LayerPage.class);
    }

    /**
     * Allows collaborating pages to update the resource info object
     * @param info
     */
    public void updateResource(ResourceInfo info) {
        myResourceModel.setObject(info);
    }
    
    /**
     * Allows collaborating pages to update the layer info object
     * @param info
     */
    public void updateLayer(LayerInfo info) {
        myLayerModel.setObject(info);
    }
}
