/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.Collections;

import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geowebcache.layer.TileLayer;

/**
 * A simple ajax link that links to the edit page for the given {@link GeoServerTileLayer} (that is,
 * either to the layerinfo edit page or layergroup edit page, as appropriate)
 * 
 */
class ConfigureCachedLayerAjaxLink extends SimpleAjaxLink<TileLayer> {

    private static final long serialVersionUID = 1L;

    private Class<? extends Page> returnPage;

    /**
     * @param id
     *            component id
     * @param itemModel
     *            model over the tile layer to configure
     * @param returnPage
     *            which page to instruct the LayerInfo or LayerGroupInfo edit page to return to
     */
    public ConfigureCachedLayerAjaxLink(String id, IModel<TileLayer> itemModel,
            Class<? extends Page> returnPage) {
        super(id, itemModel, new PropertyModel<String>(itemModel, "name"));
        this.returnPage = returnPage;
    }

    @Override
    protected void onClick(AjaxRequestTarget target) {
        final TileLayer layer = getModelObject();
        if (!(layer instanceof GeoServerTileLayer)) {
            return;
        }
        final GeoServerTileLayer geoserverTileLayer = (GeoServerTileLayer) getModelObject();
        LayerInfo layerInfo = geoserverTileLayer.getLayerInfo();
        if (layerInfo != null) {
            ResourceConfigurationPage resourceConfigPage;
            resourceConfigPage = new ResourceConfigurationPage(layerInfo, false);
            // tell the resource/layer edit page to start up on the tile cache tab
            resourceConfigPage.setSelectedTab(LayerCacheOptionsTabPanel.class);
            if (returnPage != null) {
                resourceConfigPage.setReturnPage(returnPage);
            }
            setResponsePage(resourceConfigPage);
        } else {
            PageParameters parameters = new PageParameters(Collections.singletonMap(
                    LayerGroupEditPage.GROUP, geoserverTileLayer.getName()));
            LayerGroupEditPage layerGroupEditPage = new LayerGroupEditPage(parameters);
            if (returnPage != null) {
                layerGroupEditPage.setReturnPage(returnPage);
            }
            setResponsePage(layerGroupEditPage);
        }
    }
}