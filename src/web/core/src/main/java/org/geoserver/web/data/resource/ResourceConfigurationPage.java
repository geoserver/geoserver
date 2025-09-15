/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.Constructor;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.publish.PublishedConfigurationPage;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.publish.PublishedEditTabPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.GeoTools;

/**
 * Page allowing to configure a layer and its resource.
 *
 * <p>The page is completely pluggable, the UI will be made up by scanning the Spring context for implementations of
 * {@link ResourceConfigurationPanel} and {@link PublishedConfigurationPanel}.
 *
 * <p>WARNING: one crucial aspect of this page is its ability to not loose edits when one switches from one tab to the
 * other. I did not find any effective way to unit test this, so _please_, if you do modify anything in this class
 * (especially the models), manually retest that the edits are not lost on tab switch.
 */
public class ResourceConfigurationPage extends PublishedConfigurationPage<LayerInfo> {

    @Serial
    private static final long serialVersionUID = 7870938096047218989L;

    IModel<ResourceInfo> myResourceModel;

    public ResourceConfigurationPage(PageParameters parameters) {
        this(parameters.get(WORKSPACE).toOptionalString(), parameters.get(NAME).toString());
    }

    public ResourceConfigurationPage(String workspaceName, String layerName) {
        super(false);
        this.returnPageClass = LayerPage.class;
        LayerInfo layer;
        if (workspaceName != null) {
            NamespaceInfo ns = getCatalog().getNamespaceByPrefix(workspaceName);
            if (ns == null) {
                // unlikely to happen, requires someone making modifications on the workspaces
                // with a layer page open in another tab/window
                throw new RuntimeException("Could not find workspace " + workspaceName);
            }
            layer = getCatalog().getLayerByName(ns.getName() + ":" + layerName);
        } else {
            layer = getCatalog().getLayerByName(layerName);
        }

        if (layer == null) {
            error(new ParamResourceModel("ResourceConfigurationPage.notFound", this, layerName).getString());
            setResponsePage(returnPage);
            return;
        }

        setupPublished(layer);
        setupResource(layer.getResource());
    }

    public ResourceConfigurationPage(ResourceInfo info, boolean isNew) {
        super(isNew);
        this.returnPageClass = LayerPage.class;
        setupPublished(getCatalog().getLayers(info).get(0));
        setupResource(info);
    }

    @Override
    protected void doSave(boolean doReturn) {
        super.doSave(doReturn);
    }

    public ResourceConfigurationPage(LayerInfo info, boolean isNew) {
        super(info, isNew);
        this.returnPageClass = LayerPage.class;
        setupResource(
                isNew
                        ? info.getResource()
                        : getCatalog().getResource(info.getResource().getId(), ResourceInfo.class));
    }

    private void updateResourceInLayerModel(ResourceInfo resource) {
        LayerInfo layer = getPublishedInfo();
        layer.setResource(resource);
        myModel.setObject(layer);
    }

    private void setupResource(ResourceInfo resource) {
        updateResourceInLayerModel(resource);
        myResourceModel = new CompoundPropertyModel<>(new ResourceModel(resource));
    }

    private List<ResourceConfigurationPanelInfo> filterResourcePanels(List<ResourceConfigurationPanelInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).canHandle(getResourceInfo())) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

    protected class DataLayerEditTabPanel extends ListEditTabPanel {

        @Serial
        private static final long serialVersionUID = -3442310698941800127L;

        public DataLayerEditTabPanel(String id) {
            super(id);
        }

        @Override
        protected ListView<ResourceConfigurationPanelInfo> createList(String id) {
            List<ResourceConfigurationPanelInfo> dataPanels = filterResourcePanels(
                    getGeoServerApplication().getBeansOfType(ResourceConfigurationPanelInfo.class));
            ListView<ResourceConfigurationPanelInfo> dataPanelList = new ListView<>(id, dataPanels) {

                @Serial
                private static final long serialVersionUID = -845785165778837024L;

                @Override
                protected void populateItem(ListItem<ResourceConfigurationPanelInfo> item) {
                    ResourceConfigurationPanelInfo panelInfo = item.getModelObject();
                    try {
                        final Class<ResourceConfigurationPanel> componentClass = panelInfo.getComponentClass();
                        final Constructor<ResourceConfigurationPanel> constructor =
                                componentClass.getConstructor(String.class, IModel.class);
                        ResourceConfigurationPanel panel = constructor.newInstance("content", myResourceModel);
                        item.add(panel);
                    } catch (Exception e) {
                        throw new WicketRuntimeException("Failed to add pluggable resource configuration panels", e);
                    }
                }
            };
            return dataPanelList;
        }
    }

    /** Returns the {@link ResourceInfo} contained in this page */
    public ResourceInfo getResourceInfo() {
        return myResourceModel.getObject();
    }

    /** Allows collaborating pages to update the resource info object */
    public void updateResource(ResourceInfo info) {
        updateResource(info, null);
    }

    /**
     * Allows collaborating pages to update the resource info object
     *
     * @param info the resource info to update
     */
    public void updateResource(ResourceInfo info, final AjaxRequestTarget target) {
        myResourceModel.setObject(info);
        updateResourceInLayerModel(info);
        visitChildren((component, visit) -> {
            if (component instanceof ResourceConfigurationPanel rcp) {
                rcp.resourceUpdated(target);
                visit.dontGoDeeper();
            }
        });
    }

    @Override
    protected PublishedEditTabPanel<LayerInfo> createMainTab(String panelID) {
        return new DataLayerEditTabPanel(panelID);
    }

    @Override
    protected void doSaveInternal() throws IOException {
        Catalog catalog = getCatalog();
        ResourceInfo resourceInfo = getResourceInfo();
        validateByChildren(resourceInfo);
        // allow panels to update the model in case they are not directly editing its properties
        visitChildren((component, visit) -> {
            if (component instanceof ResourceConfigurationPanel rcp) {
                rcp.onSave();
            }
        });
        visitChildren((component, visit) -> {
            if (component instanceof PublishedConfigurationPanel rcp) {
                rcp.save();
            }
        });
        if (isNew) {
            // updating grid if is a coverage
            if (resourceInfo instanceof CoverageInfo cinfo) {
                GridCoverage2DReader reader =
                        (GridCoverage2DReader) cinfo.getGridCoverageReader(null, GeoTools.getDefaultHints());

                // get bounds
                final ReferencedEnvelope bounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
                // apply the bounds, taking into account the reprojection policy if need be
                final ProjectionPolicy projectionPolicy = resourceInfo.getProjectionPolicy();
                if (projectionPolicy != ProjectionPolicy.NONE && bounds != null) {
                    // we need to fix the registered grid for this coverage
                    final GridGeometry grid = cinfo.getGrid();
                    cinfo.setGrid(new GridGeometry2D(grid.getGridRange(), grid.getGridToCRS(), resourceInfo.getCRS()));
                }
            }

            catalog.validate(resourceInfo, true).throwIfInvalid();
            LayerInfo publishedInfo = getPublishedInfo();
            catalog.add(resourceInfo);
            try {
                catalog.add(publishedInfo);
            } catch (Exception e) {
                catalog.remove(resourceInfo);
                throw e;
            }
        } else {
            ResourceInfo oldState = catalog.getResource(resourceInfo.getId(), ResourceInfo.class);

            catalog.validate(resourceInfo, true).throwIfInvalid();
            LayerInfo publishedInfo = getPublishedInfo();
            catalog.save(resourceInfo);
            try {
                LayerInfo layer = publishedInfo;
                layer.setResource(resourceInfo);
                catalog.save(layer);
            } catch (Exception e) {
                catalog.save(oldState);
                throw e;
            }
        }
    }

    private void validateByChildren(final ResourceInfo resourceInfo) {
        if (resourceInfo == null || resourceInfo.getMetadata() == null) return;
        visitChildren((component, visitor) -> {
            if (component instanceof MetadataMapValidator validator) {
                validator.validate(resourceInfo.getMetadata());
            }
        });
    }
}
